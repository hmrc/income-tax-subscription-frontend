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

import auth.agent.AuthenticatedController
import config.AppConfig
import forms.agent.AccountingYearForm
import models.AccountingYear
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import views.html.agent.business.WhatYearToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYearToSignUpController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           accountingPeriodService: AccountingPeriodService,
                                           subscriptionDetailsService: SubscriptionDetailsService,
                                           whatYearToSignUp: WhatYearToSignUp)
                                          (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                           val appConfig: AppConfig) extends AuthenticatedController {

  val backUrl: String = controllers.agent.routes.CheckYourAnswersController.show().url

  def view(accountingYearForm: Form[AccountingYear], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    whatYearToSignUp(
      accountingYearForm = accountingYearForm,
      postAction = controllers.agent.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl,
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchSelectedTaxYear() map { accountingYearModel =>
        Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYearModel.map(aym => aym.accountingYear)),
          isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingYearForm.accountingYearForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
        accountingYear => {
          Future.successful(subscriptionDetailsService.saveSelectedTaxYear(AccountingYearModel(accountingYear))) map { _ =>
            if (isEditMode) {
              Redirect(controllers.agent.routes.CheckYourAnswersController.show())
            } else {
              Redirect(controllers.agent.routes.IncomeSourceController.show())
            }
          }
        }
      )
  }
}
