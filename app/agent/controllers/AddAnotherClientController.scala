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
import agent.auth.{IncomeTaxAgentUser, StatelessController}
import agent.services.KeystoreService
import core.auth.AuthPredicate.AuthPredicate
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

@Singleton
class AddAnotherClientController @Inject()(override val baseConfig: BaseControllerConfig,
                                           override val messagesApi: MessagesApi,
                                           keystore: KeystoreService,
                                           val authService: AuthService,
                                           logging: Logging
                                          ) extends StatelessController {

  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = agent.auth.AuthPredicates.defaultPredicates

  def addAnother(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      for {
        _ <- keystore.deleteAll()
      } yield Redirect(agent.controllers.matching.routes.ClientDetailsController.show().url)
        .removingFromSession(ITSASessionKeys.JourneyStateKey)
        .removingFromSession(ITSASessionKeys.UnauthorisedAgentKey)
        .removingFromSession(ITSASessionKeys.clientData: _*)
    }.recover {
      case e =>
        logging.warn("AddAnotherClientController.addAnother encountered error: " + e)
        throw e
    }
  }

}
