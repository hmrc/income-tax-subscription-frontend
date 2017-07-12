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
import java.time.LocalDateTime._

import audit.Logging
import auth.IncomeTaxSAUser
import config.BaseControllerConfig
import connectors.models.subscription.FESuccessResponse
import connectors.models.throttling.CanAccess
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{SubscriptionService, ThrottlingService}
import uk.gov.hmrc.play.http.InternalServerException
import utils.Implicits._
import ITSASessionKeys._


import scala.concurrent.Future

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               throttlingService: ThrottlingService,
                               subscriptionService: SubscriptionService,
                               logging: Logging
                              ) extends BaseController {

  lazy val showGuidance: Boolean = baseConfig.applicationConfig.showGuidance

  def home: Action[AnyContent] = Action.async { implicit request =>
    showGuidance match {
      case true =>
        Ok(views.html.frontpage(controllers.routes.HomeController.index()))
      case _ =>
        Redirect(controllers.routes.HomeController.index())
    }
  }

  def checkAlreadySubscribed(default: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] =
    baseConfig.applicationConfig.enableCheckSubscription match {
      case false => default
      case true =>
        subscriptionService.getSubscription(user.nino.get).flatMap {
          case Some(FESuccessResponse(None)) => default
          case Some(FESuccessResponse(Some(_))) => Redirect(controllers.routes.AlreadyEnrolledController.enrolled())
          case _ => showInternalServerError
        }
    }

  def index: Action[AnyContent] = Authorised.asyncForHomeController { implicit user =>
    implicit request =>
      val timestamp: String = java.time.LocalDateTime.now().toString
      checkAlreadySubscribed(
        baseConfig.applicationConfig.enableThrottling match {
          case true =>
            throttlingService.checkAccess.flatMap {
              case Some(CanAccess) =>
                gotoPreferences.addingToSession(StartTime -> timestamp)
              case Some(_) =>
                Redirect(controllers.throttling.routes.ThrottlingController.show().url)
              case x =>
                logging.debug(s"Unexpected response from throttling service, internal server exception")
                new InternalServerException("HomeController.index: unexpected error calling the throttling service")
            }
          case false => gotoPreferences.addingToSession(StartTime -> timestamp)
        }
      )
  }

  lazy val gotoPreferences = Redirect(controllers.preferences.routes.PreferencesController.checkPreferences())
}
