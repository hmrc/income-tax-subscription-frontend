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
import views.html.agent.eligibility.CannotTakePart

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class CannotTakePartController @Inject()(val auditingService: AuditingService,
                                         val authService: AuthService)
                                        (clientDetailsRetrieval: ClientDetailsRetrieval,
                                         cannotTakePart: CannotTakePart)
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
      clientDetailsRetrieval.getClientDetails map { clientDetails =>
        Ok(cannotTakePart(
          clientName = clientDetails.name,
          clientNino = formatNino(clientDetails.nino)
        ))
      }
  }
}
