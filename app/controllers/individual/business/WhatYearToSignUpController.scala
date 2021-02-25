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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import forms.individual.business.AccountingYearForm
import javax.inject.{Inject, Singleton}
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import views.html.individual.incometax.business.what_year_to_sign_up

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYearToSignUpController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           accountingPeriodService: AccountingPeriodService,
                                           subscriptionDetailsService: SubscriptionDetailsService)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends SignUpController {

  def view(accountingYearForm: Form[AccountingYearModel], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    what_year_to_sign_up(
      accountingYearForm = accountingYearForm,
      postAction = controllers.individual.business.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl,
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchSelectedTaxYear() map { accountingYear =>
        Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYear), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingYearForm.accountingYearForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
        accountingYear => {
          subscriptionDetailsService.saveSelectedTaxYear(accountingYear) map { _ =>
            if (isEditMode) {
              Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
            } else {
              Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show())
            }
          }
        }
      )
  }

  def backUrl: String = controllers.individual.subscription.routes.CheckYourAnswersController.show().url

}
