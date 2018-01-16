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

import agent.auth.AgentJourneyState._
import agent.auth.{AgentUserMatched, IncomeTaxAgentUser, UnauthorisedAgentController}
import cats.implicits._
import core.auth.AuthPredicate.AuthPredicate
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import usermatching.utils.UserMatchingSessionUtil._

@Singleton
class AgentNotAuthorisedController @Inject()(val baseConfig: BaseControllerConfig,
                                             val messagesApi: MessagesApi,
                                             val authService: AuthService) extends UnauthorisedAgentController {

  override val unauthorisedDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] =
    agent.auth.AuthPredicates.unauthorisedUserMatchingPredicates |+| agent.auth.AuthPredicates.userMatchingJourneyPredicate |+| agent.auth.AuthPredicates.notSubmitted

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(agent.views.html.agent_not_authorised(postAction = agent.controllers.routes.AgentNotAuthorisedController.submit()))
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Redirect(agent.controllers.routes.HomeController.index()).withJourneyState(AgentUserMatched).clearUserDetails
  }

}
