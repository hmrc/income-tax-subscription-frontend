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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import forms.individual.business.AccountingYearForm
import javax.inject.{Inject, Singleton}
import models.common.{AccountingYearModel, IncomeSourceModel}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AccountingPeriodService, AuthService, SubscriptionDetailsService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYearToSignUpController @Inject()(val authService: AuthService,
                                           accountingPeriodService: AccountingPeriodService,
                                           subscriptionDetailsService: SubscriptionDetailsService
                                          )(implicit val ec: ExecutionContext, appConfig: AppConfig,
                                            mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def view(accountingYearForm: Form[AccountingYearModel], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    views.html.individual.incometax.business.what_year_to_sign_up(
      accountingYearForm = accountingYearForm,
      postAction = controllers.individual.business.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchSelectedTaxYear() map { accountingYear =>
        Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm
          .fill(accountingYear), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingYearForm.accountingYearForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
        accountingYear => {
          subscriptionDetailsService.saveSelectedTaxYear(accountingYear) flatMap { _ =>
            if (isEditMode) {
              Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show()))
            } else {
              if (isEnabled(ReleaseFour)) {
                subscriptionDetailsService.fetchIncomeSource() map {
                  case Some(IncomeSourceModel(true, _, _)) =>
                    Redirect(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
                  case Some(IncomeSourceModel(_, true, _)) =>
                    Redirect(controllers.individual.business.routes.PropertyCommencementDateController.show())
                  case Some(IncomeSourceModel(_, _, true)) =>
                    Redirect(controllers.individual.business.routes.OverseasPropertyCommencementDateController.show())
                }
              } else {
                Future.successful(Redirect(controllers.individual.business.routes.BusinessAccountingMethodController.show()))
              }
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    } else if (isEnabled(ReleaseFour)){
      controllers.individual.incomesource.routes.IncomeSourceController.show().url
    } else {
      controllers.individual.business.routes.BusinessNameController.show().url
    }
  }
}
