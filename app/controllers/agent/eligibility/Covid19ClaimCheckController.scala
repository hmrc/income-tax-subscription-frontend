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
import config.featureswitch.FeatureSwitch.RemoveCovidPages
import forms.agent.Covid19ClaimCheckForm.covid19ClaimCheckForm
import models.audits.EligibilityAnswerAuditing
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.agent.eligibility.Covid19ClaimCheck

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class Covid19ClaimCheckController @Inject()(val covid19ClaimCheck: Covid19ClaimCheck,
                                             val auditingService: AuditingService,
                                            val authService: AuthService)
                                           (implicit val ec: ExecutionContext,
                                            mcc: MessagesControllerComponents,
                                            val appConfig: AppConfig) extends StatelessController {

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      if (isEnabled(RemoveCovidPages)) {
        Redirect(routes.OtherSourcesOfIncomeController.show)
      } else {
        Ok(covid19ClaimCheck(covid19ClaimCheckForm, routes.Covid19ClaimCheckController.submit, backUrl))
      }
  }

  def submit(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val arn: Option[String] = user.arn
      covid19ClaimCheckForm.bindFromRequest.fold(
        formWithErrors => BadRequest(covid19ClaimCheck(formWithErrors, routes.Covid19ClaimCheckController.submit, backUrl)), {
          case Yes =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = false, "yes",
              "claimedCovidGrant", arn))
            Redirect(routes.CovidCannotSignUpController.show)
          case No =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = true, "no",
              "claimedCovidGrant", arn))
            Redirect(routes.OtherSourcesOfIncomeController.show)
        }
      )
  }

  def backUrl: String = {
    appConfig.incomeTaxEligibilityFrontendUrl + "/client/terms-of-participation"
  }

}
