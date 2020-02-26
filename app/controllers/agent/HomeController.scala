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

import agent.auth.AgentJourneyState._
import agent.auth._
import controllers.agent.ITSASessionKeys._
import core.config.BaseControllerConfig
import core.services.AuthService
import core.utils.Implicits._
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               val authService: AuthService
                              )(implicit val ec: ExecutionContext) extends StatelessController {

  def home: Action[AnyContent] = Action.async { implicit request =>
    Redirect(controllers.agent.routes.HomeController.index())
  }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      user.arn match {
        case Some(arn) =>
          (user.clientNino, user.clientUtr) match {
            case (Some(nino), Some(utr)) =>
              Future.successful(Redirect(controllers.agent.routes.IncomeSourceController.show()).withJourneyState(AgentSignUp))
            case (Some(nino), _) =>
              Future.successful(Redirect(controllers.agent.matching.routes.NoSAController.show()).removingFromSession(ITSASessionKeys.JourneyStateKey))
            case _ =>
              Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show())
                .addingToSession(ArnKey -> arn).withJourneyState(AgentUserMatching))
          }
        case None =>
          Future.successful(Redirect(controllers.agent.routes.NotEnrolledAgentServicesController.show()))
      }
  }

}
