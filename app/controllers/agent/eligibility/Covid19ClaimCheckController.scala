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
import forms.agent.Covid19ClaimCheckForm.covid19ClaimCheckForm
import javax.inject.{Inject, Singleton}
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import views.html.agent.eligibility.covid_19_claim_check

import scala.concurrent.ExecutionContext

@Singleton
class Covid19ClaimCheckController @Inject()(val authService: AuthService)
                                           (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                            appConfig: AppConfig) extends StatelessController {

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
        Ok(covid_19_claim_check(covid19ClaimCheckForm, routes.Covid19ClaimCheckController.submit(), backUrl))
  }

  def submit(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      covid19ClaimCheckForm.bindFromRequest.fold(
        formWithErrors => BadRequest(covid_19_claim_check(formWithErrors, routes.Covid19ClaimCheckController.submit(), backUrl)), {
          case Yes => Redirect(routes.CovidCannotSignUpController.show())
          case No => Redirect(routes.OtherSourcesOfIncomeController.show())
        }
      )
  }

  def backUrl: String = {
    appConfig.incomeTaxEligibilityFrontendUrl + "/client/terms-of-participation"
  }

}