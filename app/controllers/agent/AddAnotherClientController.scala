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

import agent.auth.{IncomeTaxAgentUser, StatelessController}
import core.auth.AuthPredicate.AuthPredicate
import core.config.BaseControllerConfig
import core.config.featureswitch.FeatureSwitching
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService
import services.agent.KeystoreService

import scala.concurrent.ExecutionContext

@Singleton
class AddAnotherClientController @Inject()(override val baseConfig: BaseControllerConfig,
                                           override val messagesApi: MessagesApi,
                                           keystore: KeystoreService,
                                           val authService: AuthService
                                          )(implicit val ec: ExecutionContext) extends StatelessController with FeatureSwitching {

  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = agent.auth.AuthPredicates.defaultPredicates

  def addAnother(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystore.deleteAll() map { _ =>
        Redirect(s"${appConfig.incomeTaxEligibilityFrontendUrl}/client/other-income")
          .removingFromSession(ITSASessionKeys.JourneyStateKey)
          .removingFromSession(ITSASessionKeys.clientData: _*)
      }
  }

}
