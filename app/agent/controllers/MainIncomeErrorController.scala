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

package agent.controllers

import javax.inject.{Inject, Singleton}

import agent.auth.AuthenticatedController
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Call, Request}

import scala.concurrent.Future

@Singleton
class MainIncomeErrorController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val authService: AuthService
                                         ) extends AuthenticatedController {


  val mainIncomeError = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(agent.views.html.main_income_error(backUrl, getAction)))
  }

  lazy val backUrl: String = agent.controllers.routes.IncomeSourceController.showIncomeSource().url
  def getAction(implicit request:Request[AnyContent]): Call = core.controllers.SignOutController.signOut(origin = routes.MainIncomeErrorController.mainIncomeError())
}
