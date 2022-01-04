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

package controllers.agent.eligibility

import auth.agent.StatelessController
import config.AppConfig
import forms.agent.AccountingPeriodCheckForm.accountingPeriodCheckForm
import javax.inject.{Inject, Singleton}
import models.audits.EligibilityAnswerAuditing
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.agent.eligibility.AccountingPeriodCheck

import scala.concurrent.ExecutionContext

@Singleton
class AccountingPeriodCheckController @Inject()(val auditingService: AuditingService,
                                                accountingPeriodCheck: AccountingPeriodCheck,
                                                val authService: AuthService)
                                               (implicit val appConfig: AppConfig,
                                                mcc: MessagesControllerComponents,
                                                val ec: ExecutionContext) extends StatelessController {

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(accountingPeriodCheck(accountingPeriodCheckForm, routes.AccountingPeriodCheckController.submit, backLink))
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val arn: Option[String] = user.arn
      accountingPeriodCheckForm.bindFromRequest.fold(
        formWithErrors => BadRequest(accountingPeriodCheck(formWithErrors, routes.AccountingPeriodCheckController.submit, backLink)),
        {
          case Yes =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = true, "yes",
              "standardAccountingPeriod", arn))
            Redirect(controllers.agent.matching.routes.ClientDetailsController.show())
          case No =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = false, "no",
              "standardAccountingPeriod", arn))
            Redirect(routes.CannotTakePartController.show)
        }
      )
  }

  def backLink: String = routes.PropertyTradingStartAfterController.show.url

}
