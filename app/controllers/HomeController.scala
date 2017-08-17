/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.{Inject, Singleton}

import audit.Logging
import auth.{AuthenticatedController, IncomeTaxSAUser}
import config.BaseControllerConfig
import connectors.models.subscription.SubscriptionResponse.SubscriptionSuccess
import controllers.ITSASessionKeys._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{AuthService, KeystoreService, SubscriptionService}
import uk.gov.hmrc.play.http.InternalServerException
import utils.Implicits._

import scala.concurrent.Future

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               subscriptionService: SubscriptionService,
                               keystoreService: KeystoreService,
                               val authService: AuthService,
                               logging: Logging
                              ) extends AuthenticatedController {

  lazy val showGuidance: Boolean = baseConfig.applicationConfig.showGuidance

  def home: Action[AnyContent] = Action { implicit request =>
    val redirect = controllers.routes.HomeController.index()

    showGuidance match {
      case true =>
        Ok(views.html.frontpage(redirect))
      case _ =>
        Redirect(redirect)
    }
  }

  private def checkAlreadySubscribed(default: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] =
        subscriptionService.getSubscription(user.nino.get).flatMap {
          case Right(None) => default
          case Right(Some(SubscriptionSuccess(mtditId))) =>
            keystoreService.saveSubscriptionId(mtditId) map { _ =>
              Redirect(controllers.routes.ClaimSubscriptionController.claim())
            }
          case _ =>
            Future.failed(new InternalServerException(s"HomeController.index: unexpected error calling the subscription service"))
        }

  def index: Action[AnyContent] = Authenticated.asyncForHomeController { implicit request =>
    implicit user =>
      val timestamp: String = java.time.LocalDateTime.now().toString
      checkAlreadySubscribed(gotoPreferences.addingToSession(StartTime -> timestamp))
  }

  lazy val gotoPreferences = Redirect(controllers.preferences.routes.PreferencesController.checkPreferences())
}
