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
import agent.services.{ClientRelationshipService, KeystoreService}
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.Future

@Singleton
class AgentNotAuthorisedController @Inject()(val baseConfig: BaseControllerConfig,
                                               val messagesApi: MessagesApi,
                                               keystoreService: KeystoreService,
                                               val authService: AuthService) extends UserMatchingController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(agent.views.html.agent_not_authorised(
        postAction = routes.AgentNotAuthorisedController.submit(),
        signOut = core.controllers.SignOutController.signOut(routes.AgentNotAuthorisedController.show())))
    )
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Redirect(routes.HomeController.index()))
  }
}
