/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.individual.eligibility.httpparsers.{Eligible, Ineligible}
import controllers.individual.eligibility.{routes => eligibilityRoutes}
import core.ITSASessionKeys._
import core.audit.Logging
import core.auth.JourneyState._
import core.auth._
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import core.utils.Implicits._
import incometax.eligibility.services.GetEligibilityStatusService
import incometax.subscription.services.SubscriptionService
import javax.inject.{Inject, Singleton}
import models.individual.subscription.SubscriptionSuccess
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.InternalServerException
import usermatching.services.{CitizenDetailsService, OptionalIdentifiers}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               subscriptionService: SubscriptionService,
                               keystoreService: KeystoreService,
                               val authService: AuthService,
                               citizenDetailsService: CitizenDetailsService,
                               getEligibilityStatusService: GetEligibilityStatusService
                              )(implicit val ec: ExecutionContext) extends StatelessController {

  def home: Action[AnyContent] = Action { implicit request =>
    val redirect = routes.HomeController.index()
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
                claimSubscription(mtditId)
              case None =>
                getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
                  case Right(Eligible) =>
                    goToSignUp(timestamp)
                      .addingToSession(UTR -> utr)
                      .addingToSession(NINO -> nino)
                  case Right(Ineligible) =>
                    Redirect(eligibilityRoutes.NotEligibleForIncomeTaxController.show())
                  case Left(_) => throw new InternalServerException(s"[HomeController] [index] Could not retrieve eligibility status")
                }
            }
          case OptionalIdentifiers(Some(_), None) =>
            goToRegistration(timestamp)
          case _ =>
            Future.successful(goToUserMatching withJourneyState UserMatching)
        }
    }

  lazy val goToPreferences = Redirect(controllers.individual.routes.PreferencesController.checkPreferences())

  lazy val goToUserMatching = Redirect(controllers.usermatching.routes.UserDetailsController.show())

  private def getSubscription(nino: String)(implicit request: Request[AnyContent]): Future[Option[SubscriptionSuccess]] =
    subscriptionService.getSubscription(nino) map {
      case Right(optionalSubscription) => optionalSubscription
      case Left(err) => throw new InternalServerException(s"HomeController.index: unexpected error calling the subscription service:\n$err")
    }

  private def goToSignUp(timestamp: String)(implicit request: Request[AnyContent]): Result =
    goToPreferences
      .addingToSession(StartTime -> timestamp)
      .withJourneyState(SignUp)

  private def goToRegistration(timestamp: String)(implicit request: Request[AnyContent]): Result =
    if (applicationConfig.enableRegistration) {
      goToPreferences
        .addingToSession(StartTime -> timestamp)
        .withJourneyState(Registration)
    } else {
      Redirect(routes.NoSAController.show())
        .removingFromSession(JourneyStateKey)
    }

  private def claimSubscription(mtditId: String)(implicit request: Request[AnyContent]): Future[Result] =
    keystoreService.saveSubscriptionId(mtditId) map { _ =>
      Redirect(controllers.individual.subscription.routes.ClaimSubscriptionController.claim())
        .withJourneyState(SignUp)
    }
}
