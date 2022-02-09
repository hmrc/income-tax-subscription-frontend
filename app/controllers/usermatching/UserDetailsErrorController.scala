/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.individual.UserMatchingController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import utilities.Implicits._
import views.html.individual.usermatching.UserDetailsError

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UserDetailsErrorController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val userDetailsError: UserDetailsError)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends UserMatchingController {

  lazy val show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      Ok(userDetailsError(postAction = controllers.usermatching.routes.UserDetailsErrorController.submit()))
  }

  lazy val submit: Action[AnyContent] = Authenticated.async { _ =>
    _ =>
      Redirect(controllers.usermatching.routes.UserDetailsController.show())
  }

}
