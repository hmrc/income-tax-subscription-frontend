/*
 * Copyright 2019 HM Revenue & Customs
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
import agent.auth._
import agent.controllers.ITSASessionKeys._
import core.config.BaseControllerConfig
import core.services.AuthService
import core.utils.Implicits._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future
import scala.concurrent.Future._

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
      user.arn match {
        case Some(arn) =>
          (user.clientNino, user.clientUtr) match {
            case (Some(nino), Some(utr)) =>
              if (request.isUnauthorisedAgent && request.isInAgentState(AgentUserMatching))
                successful(Redirect(agent.controllers.routes.AgentNotAuthorisedController.show()))
              else
                successful(Redirect(agent.controllers.routes.IncomeSourceController.show()).withJourneyState(AgentSignUp))
            case (Some(nino), _) =>
              successful(Redirect(agent.controllers.matching.routes.NoSAController.show()).removingFromSession(ITSASessionKeys.JourneyStateKey))
            case _ =>
              successful(Redirect(agent.controllers.matching.routes.ClientDetailsController.show())
                .addingToSession(ArnKey -> arn).withJourneyState(AgentUserMatching))
          }
        case None =>
          successful(Redirect(agent.controllers.routes.NotEnrolledAgentServicesController.show()))
      }
  }

}
