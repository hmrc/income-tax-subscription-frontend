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

package controllers.individual.matching

import auth.individual.JourneyState._
import auth.individual.{IncomeTaxSAUser, SignUp, StatelessController, UserIdentifiers}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys._
import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import connectors.MandationStatusConnector
import controllers.utils.ReferenceRetrieval
import models.common.subscription.SubscriptionSuccess
import models.status.MandationStatus.Mandated
import models.status.MandationStatusModel
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
        case Some(SubscriptionSuccess(_)) => // found an already existing subscription, go to claim it
          Future.successful(Redirect(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url))
        case None => // did not find a subscription, continue into sign up journey
          handleNoSubscriptionFound(utr, java.time.LocalDateTime.now().toString, nino)
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
            case EligibilityStatus(false, false, _) =>
              Future.successful(Redirect(controllers.individual.controllist.routes.NotEligibleForIncomeTaxController.show()))
            case EligibilityStatus(thisYear, _, prepopMaybe) =>
              handlePrepop(reference, prepopMaybe) flatMap { _ =>
                withMandationStatus(nino, utr) { mandationStatus =>
                  goToSignUp(thisYear)
                    .addingToSession(StartTime -> timestamp)
                    .withJourneyState(SignUp)
                    .addingToSession(UTR -> utr)
                    .addingToSession(NINO -> nino)
                    .addingToSession(ITSASessionKeys.MANDATED_CURRENT_YEAR -> (mandationStatus.currentYearStatus == Mandated).toString)
                    .addingToSession(ITSASessionKeys.MANDATED_NEXT_YEAR -> (mandationStatus.nextYearStatus == Mandated).toString)
                    .addingToSession(ELIGIBLE_NEXT_YEAR_ONLY -> (!thisYear).toString)
                }
              }
          }
        }
    }
  }

  private def withMandationStatus(nino: String, utr: String)
                                 (f: MandationStatusModel => Result)
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    mandationStatusConnector.getMandationStatus(nino, utr) map {
      case Left(_) =>
        throw new InternalServerException("[HomeController][withMandationStatus] - Unexpected failure when receiving mandation status")
      case Right(model) =>
        f(model)
    }
  }

  private def getSubscription(nino: String)(implicit request: Request[AnyContent]): Future[Option[SubscriptionSuccess]] =
    subscriptionService.getSubscription(nino) map {
      case Right(optionalSubscription) => optionalSubscription
      case Left(err) => throw new InternalServerException(s"HomeController.index: unexpected error calling the subscription service:\n$err")
    }

  private def goToSignUp(thisYear: Boolean): Result = {
    val location: Call = if (thisYear)
      controllers.individual.sps.routes.SPSHandoffController.redirectToSPS
    else
      controllers.individual.controllist.routes.CannotSignUpThisYearController.show
    Redirect(location)
  }

  private def handlePrepop(reference: String, prepopMaybe: Option[PrePopData])(implicit hc: HeaderCarrier) =
    prepopMaybe match {
      case Some(prepop) if isEnabled(PrePopulate) => prePopulationService.prePopulate(reference, prepop)
      case _ => Future.successful(())
    }
}
