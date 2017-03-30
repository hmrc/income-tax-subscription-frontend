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
import config.BaseControllerConfig
import connectors.models.throttling.CanAccess
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ThrottlingService
import uk.gov.hmrc.play.http.InternalServerException
import utils.Implicits._

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               throttlingService: ThrottlingService,
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

  def index: Action[AnyContent] = Authorised.asyncForHomeController { implicit user =>
    implicit request =>
      baseConfig.applicationConfig.enableThrottling match {
        case true =>
          throttlingService.checkAccess.flatMap {
            case Some(CanAccess) =>
              gotoPreferences
            case Some(_) =>
              Redirect(controllers.throttling.routes.ThrottlingController.show().url)
            case x =>
              logging.debug(s"Unexpected response from throttling service, internal server exception")
              new InternalServerException("HomeController.index: unexpected error calling the throttling service")
          }
        case false => gotoPreferences
      }
  }

  def agentHome : Action[AnyContent] = Action.async { implicit request =>
    showGuidance match {
      case true =>
        Ok(views.html.agent_frontpage(controllers.routes.HomeController.index()))
      case _ =>
        Redirect(controllers.routes.HomeController.index())
    }
  }


  lazy val gotoPreferences = Redirect(controllers.preferences.routes.PreferencesController.checkPreferences())
}
