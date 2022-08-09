/*
 * Copyright 2022 HM Revenue & Customs
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
import auth.individual.{IncomeTaxSAUser, SignUp, StatelessController}
import common.Constants.ITSASessionKeys._
import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import controllers.individual.eligibility.{routes => eligibilityRoutes}
import controllers.utils.ReferenceRetrieval
import models.EligibilityStatus
import models.common.subscription.SubscriptionSuccess
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
                               val mandationStatusService: MandationStatusService)
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
        getUserIdentifiers(user) flatMap {
          // No NINO, should never happen
          case (None, _, _) => throw new InternalServerException("[HomeController][index] - Could not retrieve nino from user")
          // No UTR for NINO. Not registered for self assessment
          case (_, None, _) => Future.successful(Redirect(routes.NoSAController.show).removingFromSession(JourneyStateKey))
          // Already seen this session, must have pressed back.
          case (_, Some(_), entityIdMaybe@Some(_)) if request.session.isInState(SignUp) =>
            Future.successful(Redirect(controllers.individual.sps.routes.SPSCallbackController.callback(entityIdMaybe)))
          // New user, with relevant information, try to subscribe them
          case (Some(nino), Some(utr), _) =>
            throttlingService.throttled(IndividualStartOfJourneyThrottle) {
              getSubscription(nino) flatMap {
                // Subscription available, will be throttled
                case Some(SubscriptionSuccess(mtditId)) =>
                  claimSubscription(mtditId, nino, utr)
                // New phone, who dis?
                case None => handleNoSubscriptionFound(utr, java.time.LocalDateTime.now().toString, nino)
              }
            }.map{r => r.addingToSession(UTR -> utr)}  // Add UTR, mainly for failure case so we don't look it up again.
        }
    }

  private def getUserIdentifiers(user: IncomeTaxSAUser)(implicit request: Request[AnyContent]): Future[(Option[String], Option[String], Option[String])] = {
    val maybeEntityId = request.session.data.get(SPSEntityId)
    (user.nino, user.utr, maybeEntityId) match {
      case (None, _, _) => Future.successful((None, None, None))
      case (Some(nino), Some(utr), _) => Future.successful((Some(nino), Some(utr), maybeEntityId))
      case (Some(nino), None, _) => citizenDetailsService.lookupUtr(nino).map(utrMaybe => (Some(nino), utrMaybe, maybeEntityId))
    }
  }

  private def handleNoSubscriptionFound(utr: String, timestamp: String, nino: String)
                                       (implicit hc: HeaderCarrier, user: IncomeTaxSAUser, request: Request[AnyContent]) = {
    getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
      // Check eligibility (this is complete, and gives us the control list response including pre-pop information)
      case Right(EligibilityStatus(true, _, Some(prepop))) if isEnabled(PrePopulate) =>
        withReference(utr) { reference =>
          for {
            _ <- prePopulationService.prePopulate(reference, prepop)
            _ <- mandationStatusService.retrieveMandationStatus(reference, nino, utr)
          } yield(goToSignUp(utr, timestamp, nino))
        }
      case Right(EligibilityStatus(true, _, _)) =>
        withReference(utr) { reference =>
          mandationStatusService.retrieveMandationStatus(reference, nino, utr).map { _ =>
            goToSignUp(utr, timestamp, nino)
          }
        }
      case Right(EligibilityStatus(false, _, _)) =>
        Future.successful(Redirect(eligibilityRoutes.NotEligibleForIncomeTaxController.show()))
      case Left(_) =>
        throw new InternalServerException(s"[HomeController] [index] Could not retrieve eligibility status")
    }
  }

  private def getSubscription(nino: String)(implicit request: Request[AnyContent]): Future[Option[SubscriptionSuccess]] =
    subscriptionService.getSubscription(nino) map {
      case Right(optionalSubscription) => optionalSubscription
      case Left(err) => throw new InternalServerException(s"HomeController.index: unexpected error calling the subscription service:\n$err")
    }

  private def goToSignUp(utr: String, timestamp: String, nino: String)(implicit request: Request[AnyContent]): Result =
    Redirect(controllers.individual.sps.routes.SPSHandoffController.redirectToSPS)
      .addingToSession(StartTime -> timestamp)
      .withJourneyState(SignUp)
      .addingToSession(UTR -> utr)
      .addingToSession(NINO -> nino)

  private def claimSubscription(mtditId: String, nino: String, utr: String)
                               (implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] =
    withReference(utr) {
      reference =>
        subscriptionDetailsService.saveSubscriptionId(reference, mtditId) map {
          _ =>
            Redirect(controllers.individual.subscription.routes.ClaimSubscriptionController.claim)
              .withJourneyState(SignUp)
              .addingToSession(NINO -> nino)
              .addingToSession(UTR -> utr)
        }
    }
}
