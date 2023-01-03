/*
 * Copyright 2023 HM Revenue & Customs
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
import common.Constants.ITSASessionKeys
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import utilities.UserMatchingSessionUtil.UserMatchingSessionResultUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddAnotherClientController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val appConfig: AppConfig)
                                          (implicit val ec: ExecutionContext,
                                           mcc: MessagesControllerComponents) extends StatelessController {


  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = AuthPredicates.defaultPredicates

  def addAnother(): Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Redirect(eligibility.routes.OtherSourcesOfIncomeController.show)
        .removingFromSession(ITSASessionKeys.JourneyStateKey)
        .removingFromSession(ITSASessionKeys.clientData: _*)
        .removingFromSession(ITSASessionKeys.REFERENCE)
        .clearUserName
  }

}
