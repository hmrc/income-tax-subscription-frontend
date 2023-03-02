/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.usermatching

import auth.individual.JourneyState._
import auth.individual.{IncomeTaxSAUser, SignUp, StatelessController, UserIdentifiers}
import common.Constants.ITSASessionKeys._
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ControlListYears, ItsaMandationStatus, PrePopulate}
import connectors.MandationStatusConnector
import controllers.individual.eligibility.{routes => eligibilityRoutes}
import controllers.utils.ReferenceRetrieval
import models.common.subscription.SubscriptionSuccess
import models.usermatching.CitizenDetails
import models.{EligibilityStatus, PrePopData}
import play.api.mvc._
import services._
import services.individual._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val auditingService: AuditingService,
                               val authService: AuthService,
                               citizenDetailsService: CitizenDetailsService,
                               getEligibilityStatusService: GetEligibilityStatusService,
                               val subscriptionDetailsService: SubscriptionDetailsService,
                               val prePopulationService: PrePopulationService,
                               subscriptionService: SubscriptionService,
                               throttlingService: ThrottlingService,
                               val mandationStatusConnector: MandationStatusConnector)
                              (implicit val ec: ExecutionContext,
                               val appConfig: AppConfig,
                               mcc: MessagesControllerComponents) extends StatelessController with ReferenceRetrieval {

  def home: Action[AnyContent] = Action {
    val redirect = routes.HomeController.index
    Redirect(redirect)
  }

  def index: Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        getCompletedUserIdentifiers(user) flatMap {
          // No NINO, should never happen
          case UserIdentifiers(None, _, _, _) => throw new InternalServerException("[HomeController][index] - Could not retrieve nino from user")
          // No UTR for NINO. Not registered for self assessment
          case UserIdentifiers(_, None, _, _) => Future.successful(Redirect(routes.NoSAController.show).removingFromSession(JourneyStateKey))
          // Already seen this session, must have pressed back.
          case UserIdentifiers(_, Some(_), _, entityIdMaybe@Some(_)) if request.session.isInState(SignUp) =>
            Future.successful(Redirect(controllers.individual.sps.routes.SPSCallbackController.callback(entityIdMaybe)))
          // New user, with relevant information, try to subscribe them
          case UserIdentifiers(Some(nino), Some(utr), fullName, _) => tryToSubscribe(nino, utr, fullName)
        }
    }

  private def tryToSubscribe(nino: String, utr: String, fullNameMaybe: Option[String])(implicit request: Request[AnyContent], user: IncomeTaxSAUser) =
    throttlingService.throttled(IndividualStartOfJourneyThrottle) {
      getSubscription(nino) flatMap {
        // Subscription available, will be throttled
        case Some(SubscriptionSuccess(mtditId)) => claimSubscription(mtditId, nino, utr)
        // New phone, who dis?
        case None => handleNoSubscriptionFound(utr, java.time.LocalDateTime.now().toString, nino)
      } map { response =>
        val cookiesToAdd = fullNameMaybe map (fullName => FULLNAME -> fullName)
        response.addingToSession(cookiesToAdd.toSeq: _*)
      }
    }

  private def getCompletedUserIdentifiers(user: IncomeTaxSAUser)
                                         (implicit request: Request[AnyContent]): Future[UserIdentifiers] = {
    user.getUserIdentifiersFromSession() match {
      case notCompletedLookup@UserIdentifiers(Some(nino), None, _, _) =>
        citizenDetailsService.lookupCitizenDetails(nino)
          .map { case CitizenDetails(utrMaybe, nameMaybe) => notCompletedLookup.copy(utrMaybe = utrMaybe).copy(nameMaybe = nameMaybe) }
      case userIdentifiers => Future.successful(userIdentifiers)
    }
  }

  private def handleNoSubscriptionFound(utr: String, timestamp: String, nino: String)
                                       (implicit hc: HeaderCarrier, user: IncomeTaxSAUser, request: Request[AnyContent]) = {
    getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
      // Check eligibility (this is complete, and gives us the control list response including pre-pop information)
      case Left(_) =>
        throw new InternalServerException(s"[HomeController] [index] Could not retrieve eligibility status")
      case Right(result) =>
        withReference(utr) { reference =>
          result match {
            case EligibilityStatus(false, nextYear, _) if !(nextYear && isEnabled(ControlListYears)) =>
              Future.successful(Redirect(eligibilityRoutes.NotEligibleForIncomeTaxController.show()))
            case EligibilityStatus(thisYear, _, prepopMaybe) =>
              for {
                _ <- handlePrepop(reference, prepopMaybe)
                _ <- handleMandationStatus(nino, utr)
              } yield goToSignUp(thisYear, utr, timestamp, nino)
          }
        }
    }
  }

  private def getSubscription(nino: String)(implicit request: Request[AnyContent]): Future[Option[SubscriptionSuccess]] =
    subscriptionService.getSubscription(nino) map {
      case Right(optionalSubscription) => optionalSubscription
      case Left(err) => throw new InternalServerException(s"HomeController.index: unexpected error calling the subscription service:\n$err")
    }

  private def goToSignUp(thisYear: Boolean, utr: String, timestamp: String, nino: String)(implicit request: Request[AnyContent]): Result = {
    val location: Call = if (thisYear)
      controllers.individual.sps.routes.SPSHandoffController.redirectToSPS
    else
      controllers.individual.eligibility.routes.CannotSignUpThisYearController.show
    Redirect(location)
      .addingToSession(StartTime -> timestamp)
      .withJourneyState(SignUp)
      .addingToSession(UTR -> utr)
      .addingToSession(NINO -> nino)
      .addingToSession(ELIGIBLE_NEXT_YEAR_ONLY -> (!thisYear).toString)
  }

  private def claimSubscription(mtditId: String, nino: String, utr: String)
                               (implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] =
    withReference(utr) {
      reference =>
        subscriptionDetailsService.saveSubscriptionId(reference, mtditId) map {
          case Right(_) =>
            Redirect(controllers.individual.subscription.routes.ClaimSubscriptionController.claim)
              .withJourneyState(SignUp)
              .addingToSession(NINO -> nino)
              .addingToSession(UTR -> utr)
          case Left(_) =>
            throw new InternalServerException("[HomeController][claimSubscription] - Could not save subscription id")
        }
    }

  private def handlePrepop(reference: String, prepopMaybe: Option[PrePopData])(implicit hc: HeaderCarrier) =
    prepopMaybe match {
      case Some(prepop) if isEnabled(PrePopulate) => prePopulationService.prePopulate(reference, prepop)
      case _ => Future.successful(())
    }

  private def handleMandationStatus(nino: String, utr: String)(implicit request: Request[AnyContent]): Future[Unit] = {
    if (isEnabled(ItsaMandationStatus)) {
      mandationStatusConnector.getMandationStatus(nino, utr).map(_ => ()) // to be replaced when we start using it's result
    } else {
      Future.successful(())
    }
  }

}
