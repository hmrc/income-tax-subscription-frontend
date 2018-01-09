/*
 * Copyright 2018 HM Revenue & Customs
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

package usermatching.controllers

import javax.inject.{Inject, Singleton}

import core.ITSASessionKeys._
import core.audit.Logging
import core.auth.JourneyState._
import core.auth._
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import core.utils.Implicits._
import incometax.subscription.models.SubscriptionSuccess
import incometax.subscription.services.SubscriptionService
import incometax.unauthorisedagent.services.SubscriptionStoreService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.InternalServerException
import usermatching.services.CitizenDetailsService

import scala.concurrent.Future

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               subscriptionService: SubscriptionService,
                               keystoreService: KeystoreService,
                               val authService: AuthService,
                               citizenDetailsService: CitizenDetailsService,
                               subscriptionStoreService: SubscriptionStoreService,
                               logging: Logging
                              ) extends StatelessController {

  lazy val showGuidance: Boolean = baseConfig.applicationConfig.showGuidance

  def home: Action[AnyContent] = Action { implicit request =>
    val redirect = routes.HomeController.index()

    if (showGuidance) Ok(views.html.frontpage(redirect))
    else Redirect(redirect)
  }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val timestamp: String = java.time.LocalDateTime.now().toString

      user.nino match {
        case Some(nino) =>
          getSubscription(nino) flatMap {
            case None =>
              subscriptionStoreService.retrieveSubscriptionData(nino) flatMap {
                case Some(storedSubscription) =>
                  Future.successful(goToConfirmAgentSubscription(storedSubscription.arn))
                case None =>
                  resolveUtr(user, nino) map {
                    case Some(utr) =>
                      goToSignUp(timestamp)
                        .addingToSession(UTR -> utr)
                    case None =>
                      goToRegistration(timestamp)
                  }
              }
            case Some(SubscriptionSuccess(mtditId)) =>
              claimSubscription(mtditId)
          }
        case None => goToResolveNino
      }
  }

  private def goToConfirmAgentSubscription(arn: String)(implicit request: Request[AnyContent]): Result =
    Redirect(incometax.unauthorisedagent.controllers.routes.ConfirmAgentSubscriptionController.show())
      .addingToSession(AgentReferenceNumber -> arn)

  lazy val goToPreferences = Redirect(digitalcontact.controllers.routes.PreferencesController.checkPreferences())

  lazy val goToResolveNino = Future.successful(Redirect(usermatching.controllers.routes.NinoResolverController.resolveNinoAction()))

  private def resolveUtr(user: IncomeTaxSAUser, nino: String)(implicit request: Request[AnyContent]): Future[Option[String]] =
    user.utr.fold(citizenDetailsService.lookupUtr(nino))(Future.successful(_))

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
      Redirect(incometax.subscription.controllers.routes.ClaimSubscriptionController.claim())
        .withJourneyState(SignUp)
    }
}
