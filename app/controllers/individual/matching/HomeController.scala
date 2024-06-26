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
import common.Constants.ITSASessionKeys._
import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import controllers.utils.ReferenceRetrieval
import models.audits.SignupStartedAuditing
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
class HomeController @Inject()(citizenDetailsService: CitizenDetailsService,
                               getEligibilityStatusService: GetEligibilityStatusService,
                               subscriptionService: SubscriptionService,
                               throttlingService: ThrottlingService,
                               prePopulationService: PrePopulationService)
                              (val auditingService: AuditingService,
                               val authService: AuthService,
                               val subscriptionDetailsService: SubscriptionDetailsService,
                               val sessionDataService: SessionDataService,
                               val appConfig: AppConfig)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents) extends StatelessController with ReferenceRetrieval {

  def home: Action[AnyContent] = Action {
    val redirect = routes.HomeController.index
    Redirect(redirect)
  }

  def index: Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        val userIdentifiers = getCompletedUserIdentifiers(user)
        userIdentifiers.foreach(identifiers => startIndividualSignupAudit(identifiers.utrMaybe, identifiers.ninoMaybe))
        userIdentifiers.flatMap {
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

  private def tryToSubscribe(nino: String, utr: String, fullNameMaybe: Option[String])(implicit request: Request[AnyContent]) =
    throttlingService.throttled(IndividualStartOfJourneyThrottle) {
      getSubscription(nino) flatMap {
        case Some(SubscriptionSuccess(_)) => // found an already existing subscription, go to claim it
          Future.successful(Redirect(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url))
        case None => // did not find a subscription, continue into sign up journey
          handleNoSubscriptionFound(utr, nino)
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

  private def handleNoSubscriptionFound(utr: String, nino: String)
                                       (implicit hc: HeaderCarrier, request: Request[AnyContent]) = {
    getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
      case EligibilityStatus(false, false) =>
        Future.successful(Redirect(controllers.individual.controllist.routes.NotEligibleForIncomeTaxController.show()))
      case EligibilityStatus(thisYear, _) =>
        withReference(utr, Some(nino), None) { reference =>
          handlePrepop(reference, None) map { _ =>
            goToSignUp(thisYear)
              .withJourneyState(SignUp)
              .addingToSession(UTR -> utr)
              .addingToSession(NINO -> nino)
          }
        }
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

  //TODO: re-implement when pre-pop is required
  private def handlePrepop(reference: String, prepopMaybe: Option[PrePopData])(implicit hc: HeaderCarrier) =
    prepopMaybe match {
      case Some(prepop) if isEnabled(PrePopulate) => prePopulationService.prePopulate(reference, prepop)
      case _ => Future.successful(())
    }

 private def startIndividualSignupAudit (utr: Option[String], nino: Option[String])(implicit request: Request[_]) = {
   val auditModel = SignupStartedAuditing.SignupStartedAuditModel(
      agentReferenceNumber = None,
      utr = utr,
      nino = nino
    )
    auditingService.audit(auditModel)
  }
}
