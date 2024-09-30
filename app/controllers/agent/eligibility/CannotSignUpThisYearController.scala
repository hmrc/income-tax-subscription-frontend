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

import auth.agent.PreSignUpController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService}
import views.html.agent.eligibility.CannotSignUpThisYear

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

class CannotSignUpThisYearController @Inject()(clientDetailsRetrieval: ClientDetailsRetrieval,
                                               cannotSignUp: CannotSignUpThisYear)
                                              (val auditingService: AuditingService,
                                               val authService: AuthService)
                                              (implicit val appConfig: AppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext) extends PreSignUpController {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        clientDetails <- clientDetailsRetrieval.getClientDetails
      } yield {
        Ok(cannotSignUp(
          postAction = routes.CannotSignUpThisYearController.submit,
          clientName = clientDetails.name,
          clientNino = formatNino(clientDetails.nino)
        ))
      }
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    _ => Redirect(controllers.agent.routes.UsingSoftwareController.show)
  }
}
