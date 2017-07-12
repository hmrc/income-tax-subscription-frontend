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

package controllers.throttling

import javax.inject.{Inject, Singleton}

import config.BaseControllerConfig
import controllers.AuthenticatedController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService

@Singleton
class ThrottlingController @Inject()(override val baseConfig: BaseControllerConfig,
                                     val messagesApi: MessagesApi,
                                     val authService: AuthService
                                    ) extends AuthenticatedController {

  val show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(views.html.throttling.daily_limit_reached(controllers.throttling.routes.ThrottlingController.submit()))
  }

  val submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user => Redirect(controllers.routes.SignOutController.signOut())
  }

}
