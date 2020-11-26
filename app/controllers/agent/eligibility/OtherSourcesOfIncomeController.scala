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

package controllers.agent.eligibility

import auth.agent.StatelessController
import config.AppConfig
import forms.agent.OtherSourcesOfIncomeForm.otherSourcesOfIncomeForm
import javax.inject.{Inject, Singleton}
import models.audits.EligibilityAnswerAuditing
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.agent.eligibility.other_sources_of_income

import scala.concurrent.ExecutionContext

@Singleton
class OtherSourcesOfIncomeController @Inject()(auditService: AuditingService,
                                               val authService: AuthService)
                                              (implicit appConfig: AppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext) extends StatelessController {

  def backUrl: String = routes.Covid19ClaimCheckController.show().url

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(other_sources_of_income(otherSourcesOfIncomeForm, routes.OtherSourcesOfIncomeController.submit(), backUrl))
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val arn: Option[String] = user.arn
      otherSourcesOfIncomeForm.bindFromRequest.fold(
        formWithErrors => BadRequest(other_sources_of_income(formWithErrors, routes.OtherSourcesOfIncomeController.submit(), backUrl)),
        {
          case Yes =>
            auditService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, false, "yes",
              "otherIncomeSource", arn))
            Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show())
          case No =>
            auditService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, true, "no",
              "otherIncomeSource", arn))
            Redirect(controllers.agent.eligibility.routes.SoleTraderController.show())
        }
      )
  }

}
