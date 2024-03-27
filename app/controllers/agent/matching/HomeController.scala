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

package controllers.agent.matching

import auth.agent.AgentJourneyState._
import auth.agent._
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.JourneyStateKey
import config.AppConfig
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
    Future.successful(Redirect(controllers.agent.matching.routes.HomeController.index))
  }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      if (request.session.get(JourneyStateKey).contains(AgentUserMatching.name)) {
        Future.successful(Redirect(routes.ClientDetailsController.show()))
      } else if (request.session.get(JourneyStateKey).contains(AgentSignUp.name)) {
        if (request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).contains("true")) {
          Future.successful(Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show))
        } else {
          Future.successful(Redirect(controllers.agent.eligibility.routes.ClientCanSignUpController.show()))
        }
      } else {
        Future.successful(Redirect(controllers.agent.routes.AddAnotherClientController.addAnother()))
      }
  }

  implicit val cacheSessionFunctions: Session => SessionFunctions = AgentJourneyState.SessionFunctions

}
