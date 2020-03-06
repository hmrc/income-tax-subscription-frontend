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

package controllers.individual.incomesource

import core.audit.Logging
import core.auth.SignUpController
import core.config.BaseControllerConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService
import services.individual.KeystoreService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CannotSignUpController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val logging: Logging,
                                       val authService: AuthService)
                                      (implicit val ec: ExecutionContext) extends SignUpController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.individual.incometax.incomesource.cannot_sign_up(
        postAction = controllers.individual.incomesource.routes.CannotSignUpController.show(),
        backUrl
      )))
  }

  lazy val backUrl: String = controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url
}
