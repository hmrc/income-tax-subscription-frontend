/*
 * Copyright 2021 HM Revenue & Customs
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


import auth.agent.PostSubmissionController
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import utilities.SubscriptionDataUtil._
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.sign_up_complete

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class ConfirmationController @Inject()(val auditingService: AuditingService,
                                       val authService: AuthService,
                                       accountingPeriodService: AccountingPeriodService,
                                       subscriptionDetailsService: SubscriptionDetailsService)
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends PostSubmissionController {


  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }

  }

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      val postAction = controllers.agent.routes.AddAnotherClientController.addAnother()
      val signOutAction = controllers.SignOutController.signOut(origin = routes.ConfirmationController.show())
      val endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear
      val updatesAfter = accountingPeriodService.updateDatesAfter()
      val updatesBefore = accountingPeriodService.updateDatesBefore()
      val clientName = request.fetchClientName.getOrElse(throw new Exception("[ConfirmationController][show]-could not retrieve client name from session"))
      val clientNino = user.clientNino.getOrElse(throw new Exception("[ConfirmationController][show]-could not retrieve client nino from session"))

      val formattedClientNino = formatNino(clientNino)

      subscriptionDetailsService.fetchAll() map { cacheMap =>
        Ok(sign_up_complete(cacheMap.getAgentSummary(), clientName, formattedClientNino, endYearOfCurrentTaxPeriod, updatesBefore, updatesAfter, postAction, signOutAction))
      }
  }

}
