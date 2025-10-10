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

package controllers.agent.eligibility

import auth.agent.AgentSignUp
import common.Constants.ITSASessionKeys.JourneyStateKey
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.eligibility.ClientCanSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ClientCanSignUpController @Inject()(identify: IdentifierAction,
                                          journeyRefiner: ConfirmedClientJourneyRefiner,
                                          subscriptionDetailsService: SubscriptionDetailsService,
                                          view: ClientCanSignUp)
                                         (implicit mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) { implicit request =>
        Ok(view(
          routes.ClientCanSignUpController.submit(),
          backUrl = backUrl,
          clientName = request.clientDetails.name,
          clientNino = request.clientDetails.formattedNino
        ))
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.saveEligibilityInterruptPassed(request.reference) map {
      case Right(_) =>
        Redirect(controllers.agent.routes.UsingSoftwareController.show(false))
          .addingToSession(JourneyStateKey -> AgentSignUp.name)
      case Left(_) =>
        throw new InternalServerException("[ClientCanSignUpController][continueToSignUpClient] - Failed to save eligibility interrupt passed")
    }
  }

  def backUrl: String = {
    controllers.agent.matching.routes.ConfirmClientController.show().url
  }
}
