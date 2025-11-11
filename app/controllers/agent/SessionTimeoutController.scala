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

package controllers.agent

import config.AppConfig
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.agent.Timeout

import javax.inject.{Inject, Singleton}

@Singleton
class SessionTimeoutController @Inject()(val agentTimeout: Timeout, mcc: MessagesControllerComponents, appConfig: AppConfig) extends FrontendController(mcc) {

  val show: Action[AnyContent] = Action { implicit request =>
    Ok(agentTimeout())
  }

  val keepAlive: Action[AnyContent] = Action { implicit request =>
    Ok.withSession(request.session)
  }

  val timeout: Action[AnyContent] = Action {
    appConfig.redirectToLogin(controllers.agent.matching.routes.HomeController.home.url)
  }

}
