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
import agent.auth.AgentJourneyState._
import agent.auth.{AgentSignUp, StatelessController, AgentUserMatched, AgentUserMatching}
import core.config.BaseControllerConfig
import agent.controllers.ITSASessionKeys._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import core.services.AuthService
import core.utils.Implicits._

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               val authService: AuthService,
                               logging: Logging
                              ) extends StatelessController {

  lazy val showGuidance: Boolean = baseConfig.applicationConfig.showGuidance

  def home: Action[AnyContent] = Action.async { implicit request =>
    showGuidance match {
      case true => Ok(agent.views.html.agent_frontpage(agent.controllers.routes.HomeController.index()))
      case _ => Redirect(agent.controllers.routes.HomeController.index())
    }
  }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (request.isInAgentState(AgentUserMatched))
        Redirect(agent.controllers.routes.IncomeSourceController.showIncomeSource()).withJourneyState(AgentSignUp)
      else
        user.arn match {
          case Some(arn) =>
            Redirect(agent.controllers.matching.routes.ClientDetailsController.show())
              .addingToSession(ArnKey -> arn).withJourneyState(AgentUserMatching)
          case None =>
            Redirect(agent.controllers.routes.NotEnrolledAgentServicesController.show())
        }
  }

}
