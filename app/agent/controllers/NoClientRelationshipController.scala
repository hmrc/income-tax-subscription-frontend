/*
 * Copyright 2018 HM Revenue & Customs
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

import agent.auth.UserMatchingController
import core.config.BaseControllerConfig
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import agent.services.{ClientRelationshipService, KeystoreService}
import core.services.AuthService

import scala.concurrent.Future

@Singleton
class NoClientRelationshipController @Inject()(val baseConfig: BaseControllerConfig,
                                               val messagesApi: MessagesApi,
                                               clientRelationshipService: ClientRelationshipService,
                                               keystoreService: KeystoreService,
                                               val authService: AuthService) extends UserMatchingController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(agent.views.html.no_client_relationship(postAction = agent.controllers.routes.NoClientRelationshipController.submit())))
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Redirect(agent.controllers.matching.routes.ClientDetailsController.show()))
  }
}
