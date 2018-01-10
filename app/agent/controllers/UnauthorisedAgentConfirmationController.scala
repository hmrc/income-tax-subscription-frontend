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

import agent.audit.Logging
import agent.auth.PostSubmissionController
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.UnauthorisedAgentFeature
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.NotFoundException

@Singleton
class UnauthorisedAgentConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                                        val messagesApi: MessagesApi,
                                                        val keystoreService: KeystoreService,
                                                        val authService: AuthService,
                                                        val logging: Logging
                                                       ) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      if (applicationConfig.isEnabled(UnauthorisedAgentFeature)) {
        Ok(agent.views.html.unauthorised_agent_confirmation(
          postAction = agent.controllers.routes.AddAnotherClientController.addAnother()
        ))
      } else {
        throw new NotFoundException("Cannot access unauthorised agent confirmation when feature switch not enabled")
      }
  }
}
