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
import forms.agent.AccountingPeriodCheckForm.accountingPeriodCheckForm
import javax.inject.{Inject, Singleton}
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import views.html.agent.eligibility.accounting_period_check

import scala.concurrent.ExecutionContext

@Singleton
class AccountingPeriodCheckController @Inject()(val authService: AuthService)
                                               (implicit appConfig: AppConfig,
                                                mcc: MessagesControllerComponents,
                                                val ec: ExecutionContext) extends StatelessController {

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(accounting_period_check(accountingPeriodCheckForm, routes.AccountingPeriodCheckController.submit(), backLink))
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      accountingPeriodCheckForm.bindFromRequest.fold(
        formWithErrors => BadRequest(accounting_period_check(formWithErrors, routes.AccountingPeriodCheckController.submit(), backLink)),
        {
          case Yes => Redirect(controllers.agent.matching.routes.ClientDetailsController.show())
          case No => Redirect(routes.CannotTakePartController.show())
        }
      )
  }

  def backLink: String = routes.PropertyTradingStartAfterController.show().url

}
