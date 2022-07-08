/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.agent.AgentJourneyState._
import auth.agent.{AgentJourneyState, AgentSignUp, AgentUserMatching, StatelessController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.ArnKey
import config.AppConfig
import play.api.mvc._
import services.{AgentStartOfJourneyThrottle, AuditingService, AuthService, ThrottlingService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val auditingService: AuditingService,
                               val authService: AuthService,
                               val appConfig: AppConfig,
                               throttlingService: ThrottlingService)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents) extends StatelessController {

  def home: Action[AnyContent] = Action.async {
    Future.successful(Redirect(controllers.agent.routes.HomeController.index))
  }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val alreadyInSignUp = request.session.isInState(AgentSignUp)
      val ninoPresent = user.clientNino.isDefined
      val utrPresent = user.clientUtr.isDefined
      val arnMaybe = user.arn
      (alreadyInSignUp, ninoPresent, utrPresent, arnMaybe) match {
        // this session has already passed through the throttle
        case (true, _, _, _) => Future.successful(Redirect(controllers.agent.routes.TaskListController.show()))
        // this session is new, has full data
        case (_, true, true, _) =>
          throttlingService.throttled(AgentStartOfJourneyThrottle) {
            Future.successful(Redirect(controllers.agent.routes.TaskListController.show()).withJourneyState(AgentSignUp))
          }
        // this session has missing data
        case (_, true, _, _) =>
          Future.successful(Redirect(controllers.agent.matching.routes.NoSAController.show).removingFromSession(ITSASessionKeys.JourneyStateKey))
        // Got an agent enrolment only - no user data at all
        case (_, _, _, Some(arn)) =>
          Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show())
            .addingToSession(ArnKey -> arn)
            .withJourneyState(AgentUserMatching))
        // Got nothing. Are you even enrolled?
        case (_, _, _, _) => Future.successful(Redirect(controllers.agent.routes.NotEnrolledAgentServicesController.show))
      }
  }

  implicit val cacheSessionFunctions: Session => SessionFunctions = AgentJourneyState.SessionFunctions
  implicit val cacheRequestFunctions: Request[_] => RequestFunctions = AgentJourneyState.RequestFunctions

}
