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
import common.Constants.ITSASessionKeys
import config.AppConfig
import forms.agent.AccountingPeriodCheckForm.accountingPeriodCheckForm
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.eligibility.AccountingPeriodCheck

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class AccountingPeriodCheckController @Inject()(val auditingService: AuditingService,
                                                accountingPeriodCheck: AccountingPeriodCheck,
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

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(accountingPeriodCheck(
        accountingPeriodCheckForm,
        routes.AccountingPeriodCheckController.submit,
        clientName = request.fetchClientName.getOrElse(
          throw new InternalServerException("[AccountingPeriodCheckController][show] - could not retrieve client name from session")
        ),
        clientNino = formatNino(user.clientNino.getOrElse(
          throw new InternalServerException("[AccountingPeriodCheckController][show] - could not retrieve client nino from session")
        )),
        backLink))
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val clientName = request.fetchClientName.get
      val clientNino = user.clientNino.get
      val nextYearOnly = request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).contains("true")
      accountingPeriodCheckForm.bindFromRequest().fold(
        formWithErrors => BadRequest(accountingPeriodCheck(formWithErrors, routes.AccountingPeriodCheckController.submit, clientName, clientNino, backLink)),
        {
          case Yes =>
            auditingService.audit(EligibilityAnswerAuditModel(eligible = true, "yes", "standardAccountingPeriod", user.arn))
            if (nextYearOnly)
              Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show)
            else
              Redirect(controllers.agent.matching.routes.HomeController.home)
          case No =>
            auditingService.audit(EligibilityAnswerAuditModel(eligible = false, "no", "standardAccountingPeriod", user.arn))
            Redirect(routes.CannotTakePartController.show)
        }
      )
  }

  def backLink: String = controllers.agent.matching.routes.ReturnToClientDetailsController.show.url

}
