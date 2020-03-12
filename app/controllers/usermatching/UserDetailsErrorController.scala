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

import core.auth.UserMatchingController
import core.config.AppConfig
import core.utils.Implicits._
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService

import scala.concurrent.ExecutionContext

@Singleton
class UserDetailsErrorController @Inject()(val authService: AuthService,
                                           val messagesApi: MessagesApi)
                                          (implicit val ec: ExecutionContext, appConfig: AppConfig) extends UserMatchingController {

  lazy val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Ok(views.html.individual.usermatching.user_details_error(
        postAction = controllers.usermatching.routes.UserDetailsErrorController.submit()
      ))
  }

  lazy val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Redirect(controllers.usermatching.routes.UserDetailsController.show())
  }

}
