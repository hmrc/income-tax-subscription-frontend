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

package controllers.agent

import auth.agent.AuthenticatedController
import config.AppConfig
import play.api.mvc._
import services.{AuditingService, AuthService, GetEligibilityStatusService, MandationStatusService}
import views.html.agent.WhatYouNeedToDo

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class WhatYouNeedToDoController @Inject()(whatYouNeedToDo: WhatYouNeedToDo,
                                          eligibilityStatusService: GetEligibilityStatusService,
                                          mandationStatusService: MandationStatusService)
                                         (val auditingService: AuditingService,
                                          val appConfig: AppConfig,
                                          val authService: AuthService)
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends AuthenticatedController {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        eligibilityStatus <- eligibilityStatusService.getEligibilityStatus(user.getClientUtr)
        mandationStatus <- mandationStatusService.getMandationStatus(user.getClientNino, user.getClientUtr)
      } yield {
        Ok(whatYouNeedToDo(
          postAction = routes.WhatYouNeedToDoController.submit,
          eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly,
          mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
          mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
          clientName = user.clientName,
          clientNino = formatNino(user.getClientNino)
        ))
      }
  }

  val submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      Redirect(controllers.agent.tasklist.routes.TaskListController.show())
  }

}
