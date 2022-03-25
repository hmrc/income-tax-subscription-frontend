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
import auth.individual.{IncomeTaxSAUser, SignUp, StatelessController, UserMatching}
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
import utilities.ITSASessionKeys._
import utilities.Implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val auditingService: AuditingService,
                               val authService: AuthService,
                               citizenDetailsService: CitizenDetailsService,
                               getEligibilityStatusService: GetEligibilityStatusService,
                               val subscriptionDetailsService: SubscriptionDetailsService,
                               val prePopulationService: PrePopulationService,
                               subscriptionService: SubscriptionService)
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
        val timestamp: String = java.time.LocalDateTime.now().toString
        citizenDetailsService.resolveKnownFacts(user.nino, user.utr) flatMap {
          case OptionalIdentifiers(Some(nino), Some(utr)) =>
            getSubscription(nino) flatMap {
              case Some(SubscriptionSuccess(mtditId)) =>
                claimSubscription(mtditId, nino, utr)
              case None =>
                handleNoSubscriptionFound(utr, timestamp, nino)
            }
          case OptionalIdentifiers(Some(_), None) =>
            Redirect(routes.NoSAController.show)
              .removingFromSession(JourneyStateKey)
          case _ =>
            Future.successful(goToUserMatching withJourneyState UserMatching)
        }
    }

  private def handleNoSubscriptionFound(utr: String, timestamp: String, nino: String)
                                       (implicit hc: HeaderCarrier, user: IncomeTaxSAUser, request: Request[AnyContent]) = {
    getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
      // Check eligibility (this is complete, and gives us the control list response including pre-pop information)
      case Right(EligibilityStatus(true, _, Some(prepop))) if isEnabled(PrePopulate) =>
        withReference(utr) { reference =>
          prePopulationService.prePopulate(reference, prepop).map { _ =>
            goToSignUp(utr, timestamp, nino)
          }
        }
      case Right(EligibilityStatus(true, _, _)) => goToSignUp(utr, timestamp, nino)
      case Right(EligibilityStatus(false, _, _)) =>
        Redirect(eligibilityRoutes.NotEligibleForIncomeTaxController.show())
      case Left(_) =>
        throw new InternalServerException(s"[HomeController] [index] Could not retrieve eligibility status")
    }
  }

  lazy val goToPreferences: Result = Redirect(controllers.individual.routes.PreferencesController.checkPreferences)
  lazy val goToSPSHandoff: Result = Redirect(controllers.individual.sps.routes.SPSHandoffController.redirectToSPS)

  lazy val goToUserMatching: Result = Redirect(controllers.usermatching.routes.UserDetailsController.show())

  private def getSubscription(nino: String)(implicit request: Request[AnyContent]): Future[Option[SubscriptionSuccess]] =
    subscriptionService.getSubscription(nino) map {
      case Right(optionalSubscription) => optionalSubscription
      case Left(err) => throw new InternalServerException(s"HomeController.index: unexpected error calling the subscription service:\n$err")
    }

  private def goToSignUp(utr: String, timestamp: String, nino: String)(implicit request: Request[AnyContent]): Result =
    goToSPSHandoff
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
