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
import forms.agent.OtherSourcesOfIncomeForm.otherSourcesOfIncomeForm
import javax.inject.{Inject, Singleton}
import models.audits.EligibilityAnswerAuditing
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.agent.eligibility.OtherSourcesOfIncome

import scala.concurrent.ExecutionContext

@Singleton
class OtherSourcesOfIncomeController @Inject()(otherSourcesOfIncome: OtherSourcesOfIncome,
                                                val auditingService: AuditingService,
                                               val authService: AuthService)
                                              (implicit val appConfig: AppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext) extends StatelessController  {

  def backUrl: String = appConfig.incomeTaxEligibilityFrontendUrl + "/client/terms-of-participation"

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Ok(otherSourcesOfIncome(otherSourcesOfIncomeForm, routes.OtherSourcesOfIncomeController.submit, backUrl))
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val arn: Option[String] = user.arn
      otherSourcesOfIncomeForm.bindFromRequest.fold(
        formWithErrors => BadRequest(otherSourcesOfIncome(formWithErrors, routes.OtherSourcesOfIncomeController.submit, backUrl)),
        {
          case Yes =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = false, "yes",
              "otherIncomeSource", arn))
            Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show)
          case No =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = true, "no",
              "otherIncomeSource", arn))
            Redirect(controllers.agent.eligibility.routes.SoleTraderController.show)
        }
      )
  }

}
