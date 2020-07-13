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

import auth.agent.{AuthPredicates, IncomeTaxAgentUser, StatelessController}
import auth.individual.AuthPredicate.AuthPredicate
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import services.KeystoreService

import scala.concurrent.ExecutionContext

@Singleton
class AddAnotherClientController @Inject()(val authService: AuthService, appConfig: AppConfig, keystore: KeystoreService)(
  implicit val ec: ExecutionContext, mcc: MessagesControllerComponents) extends StatelessController {

  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = AuthPredicates.defaultPredicates

  def addAnother(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystore.deleteAll() map { _ =>
        Redirect(s"${appConfig.incomeTaxEligibilityFrontendUrl}/client/covid-19")
          .removingFromSession(ITSASessionKeys.JourneyStateKey)
          .removingFromSession(ITSASessionKeys.clientData: _*)
      }
  }

}
