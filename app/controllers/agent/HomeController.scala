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
import auth.agent.{AgentSignUp, AgentUserMatching, StatelessController}
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.agent.ITSASessionKeys._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import utilities.Implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val auditingService: AuditingService,
                               val authService: AuthService,
                               val appConfig: AppConfig)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents) extends StatelessController {

  def home: Action[AnyContent] = Action.async {
    Redirect(controllers.agent.routes.HomeController.index)
  }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      user.arn match {
        case Some(arn) =>
          (user.clientNino, user.clientUtr) match {
            case (Some(_), Some(_)) =>
              if(isEnabled(SaveAndRetrieve)) {
                Future.successful(Redirect(controllers.agent.routes.TaskListController.show()).withJourneyState(AgentSignUp))
              } else {
                Future.successful(Redirect(controllers.agent.routes.WhatYearToSignUpController.show()).withJourneyState(AgentSignUp))
              }
            case (Some(_), _) =>
              Future.successful(Redirect(controllers.agent.matching.routes.NoSAController.show).removingFromSession(ITSASessionKeys.JourneyStateKey))
            case _ =>
              Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show())
                .addingToSession(ArnKey -> arn).withJourneyState(AgentUserMatching))
          }
        case None =>
          Future.successful(Redirect(controllers.agent.routes.NotEnrolledAgentServicesController.show))
      }
  }

}
