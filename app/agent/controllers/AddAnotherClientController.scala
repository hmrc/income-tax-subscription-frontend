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

package agent.controllers

import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.auth.PostSubmissionController
import agent.config.BaseControllerConfig
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import agent.services.{AuthService, KeystoreService}

@Singleton
class AddAnotherClientController @Inject()(override val baseConfig: BaseControllerConfig,
                                           override val messagesApi: MessagesApi,
                                           keystore: KeystoreService,
                                           val authService: AuthService,
                                           logging: Logging
                                          ) extends PostSubmissionController {

  def addAnother(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      for {
        _ <- keystore.deleteAll()
      } yield Redirect(agent.controllers.matching.routes.ClientDetailsController.show().url).removingFromSession(ITSASessionKeys.Submitted, ITSASessionKeys.JourneyStateKey)
    }.recover {
      case e =>
        logging.warn("AddAnotherClientController.addAnother encountered error: " + e)
        throw e
    }
  }

}
