/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.agent.UserMatchingController
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientAlreadySubscribedController @Inject()(val authService: AuthService)(implicit val ec: ExecutionContext, appConfig: AppConfig,
                                                  mcc: MessagesControllerComponents) extends UserMatchingController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.agent.client_already_subscribed(
        postAction = controllers.agent.routes.ClientAlreadySubscribedController.submit()
      )))
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show()))
  }

}
