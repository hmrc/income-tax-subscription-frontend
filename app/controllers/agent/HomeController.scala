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
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.agent.ITSASessionKeys._
import play.api.mvc._
import services.{AuditingService, AuthService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val auditingService: AuditingService,
                               val authService: AuthService,
                               val appConfig: AppConfig)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents) extends StatelessController {

  def home: Action[AnyContent] = Action.async {
    Future.successful(Redirect(controllers.agent.routes.HomeController.index))
  }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val alreadyInSignUp = request.session.isInState(AgentSignUp)
      val saveAndRetrieve = isEnabled(SaveAndRetrieve)
      val ninoPresent = user.clientNino.isDefined
      val utrPresent = user.clientUtr.isDefined
      val arnMaybe = user.arn
      val redirect = (alreadyInSignUp, saveAndRetrieve, ninoPresent, utrPresent, arnMaybe) match {
        // this session has already passed through the throttle, and we are in S&R
        case (true, true, _, _, _) => Redirect(controllers.agent.routes.TaskListController.show())
        // this session has already passed through the throttle and we are not in S&R
        case (true, _, _, _, _) => Redirect(controllers.agent.routes.WhatYearToSignUpController.show())
        // this session is new, has full data, and we are in S&R
        case (_, true, true, true, _) => Redirect(controllers.agent.routes.TaskListController.show()).withJourneyState(AgentSignUp)
        // this session is new, has full data, and we are not in S&R
        case (_, _, true, true, _) => Redirect(controllers.agent.routes.WhatYearToSignUpController.show()).withJourneyState(AgentSignUp)
        // this session has missing data
        case (_, _, true, _, _) => Redirect(controllers.agent.matching.routes.NoSAController.show).removingFromSession(ITSASessionKeys.JourneyStateKey)
        // Got an agent enrolment only - no user data at all
        case (_, _, _, _, Some(arn)) =>
          Redirect(controllers.agent.matching.routes.ClientDetailsController.show()).addingToSession(ArnKey -> arn).withJourneyState(AgentUserMatching)
        // Got nothing. Are you even enrolled?
        case (_, _, _, _, _) => Redirect(controllers.agent.routes.NotEnrolledAgentServicesController.show)
      }
      Future.successful(redirect)
  }

  implicit val cacheSessionFunctions: Session => SessionFunctions = AgentJourneyState.SessionFunctions
  implicit val cacheRequestFunctions: Request[_] => RequestFunctions = AgentJourneyState.RequestFunctions

}
