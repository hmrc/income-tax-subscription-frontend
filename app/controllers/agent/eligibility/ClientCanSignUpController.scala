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

import auth.agent.{AgentSignUp, IncomeTaxAgentUser, PreSignUpController}
import common.Constants.ITSASessionKeys.JourneyStateKey
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.agent.ClientCanSignUpForm.clientCanSignUpForm
import models.{No, Yes}
import play.api.mvc._
import services.{AuditingService, AuthService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.eligibility.ClientCanSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class ClientCanSignUpController @Inject()(val auditingService: AuditingService,
                                          clientCanSignUp: ClientCanSignUp,
                                          val subscriptionDetailsService: SubscriptionDetailsService,
                                          val sessionDataService: SessionDataService,
                                          val authService: AuthService)
                                         (implicit val appConfig: AppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext) extends PreSignUpController with ReferenceRetrieval {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(clientCanSignUp(
        clientCanSignUpForm,
        routes.ClientCanSignUpController.submit(),
        clientName = request.fetchClientName.getOrElse(
          throw new InternalServerException("[ClientCanSignUpController][show] - could not retrieve client name from session")
        ),
        clientNino = formatNino(user.clientNino.getOrElse(
          throw new InternalServerException("[ClientCanSignUpController][show] - could not retrieve client nino from session")
        )),
        backLink))
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val clientName: String = request.fetchClientName.getOrElse(
        throw new InternalServerException("[ClientCanSignUpController][submit] - could not retrieve client name from session")
      )
      val clientNino: String = formatNino(user.clientNino.getOrElse(
        throw new InternalServerException("[ClientCanSignUpController][submit] - could not retrieve client nino from session")
      ))
      clientCanSignUpForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(clientCanSignUp(
            formWithErrors,
            routes.ClientCanSignUpController.submit(),
            clientName,
            clientNino,
            backLink
          ))),
        {
          case Yes =>
            continueToSignUpClient
          case No =>
            Future.successful(Redirect(controllers.agent.routes.AddAnotherClientController.addAnother()))
        }
      )
  }

  private def continueToSignUpClient(implicit request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    withAgentReference { reference =>
      subscriptionDetailsService.saveEligibilityInterruptPassed(reference) map {
        case Right(_) =>
          Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
            .addingToSession(JourneyStateKey -> AgentSignUp.name)
        case Left(_) =>
          throw new InternalServerException("[ClientCanSignUpController][continueToSignUpClient] - Failed to save eligibility interrupt passed")
      }
    }

  }

  def backLink: String = controllers.agent.routes.AddAnotherClientController.addAnother().url

}
