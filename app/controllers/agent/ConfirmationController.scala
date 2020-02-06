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

package controllers.agent

import agent.audit.Logging
import agent.auth.PostSubmissionController
import agent.services.CacheUtil._
import agent.services.KeystoreService
import views.html.agent.sign_up_complete
import core.config.BaseControllerConfig
import core.services.AuthService
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

@Singleton
class ConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService,
                                       val logging: Logging
                                      ) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val postAction = controllers.agent.routes.AddAnotherClientController.addAnother()
      val signOutAction = controllers.SignOutController.signOut(origin = routes.ConfirmationController.show())
      keystoreService.fetchAll() map (_.get.getSummary()) map {
        agentSummary => Ok(sign_up_complete(agentSummary, postAction, signOutAction))
      }
  }

}
