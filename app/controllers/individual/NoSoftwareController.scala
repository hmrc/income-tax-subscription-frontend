/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.individual

import auth.individual.SignUpController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.individual.NoSoftware

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class NoSoftwareController @Inject()(noSoftware: NoSoftware)
                                    (val auditingService: AuditingService,
                                     val appConfig: AppConfig,
                                     val authService: AuthService)
                                    (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext)
  extends SignUpController {

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Ok(noSoftware(
        backUrl = controllers.individual.routes.UsingSoftwareController.show().url
      ))
  }
}
