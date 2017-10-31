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

package agent.controllers.matching

import javax.inject.{Inject, Singleton}

import agent.auth.UserMatchingController
import core.config.BaseControllerConfig
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import core.services.AuthService
import core.utils.Implicits._

@Singleton
class ClientDetailsErrorController @Inject()(val baseConfig: BaseControllerConfig,
                                             val messagesApi: MessagesApi,
                                             val authService: AuthService
                                            ) extends UserMatchingController {

  lazy val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Ok(agent.views.html.client_details_error(postAction = agent.controllers.matching.routes.ClientDetailsErrorController.submit()))
  }

  lazy val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Redirect(agent.controllers.matching.routes.ClientDetailsController.show())
  }

}
