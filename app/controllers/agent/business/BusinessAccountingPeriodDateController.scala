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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import forms.agent.AccountingPeriodDateForm
import javax.inject.{Inject, Singleton}
import models.common.IncomeSourceModel
import models.individual.business.AccountingPeriodModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AccountingPeriodService, AuthService, SubscriptionDetailsService}
import utilities.AccountingPeriodUtil
import utilities.Implicits._
import utilities.SubscriptionDataUtil._

import scala.concurrent.ExecutionContext

@Singleton
class BusinessAccountingPeriodDateController @Inject()(val authService: AuthService,
                                                       accountingPeriodService: AccountingPeriodService,
                                                       subscriptionDetailsService: SubscriptionDetailsService)(
                                                       implicit val ec: ExecutionContext,
                                                       mcc: MessagesControllerComponents,
                                                       appConfig: AppConfig) extends AuthenticatedController {

  def view(form: Form[AccountingPeriodModel], backUrl: String, isEditMode: Boolean)(implicit request: Request[_]): Html = {
    views.html.agent.business.accounting_period_date(
      form,
      controllers.agent.business.routes.BusinessAccountingPeriodDateController.submit(editMode = isEditMode),
      isEditMode,
      backUrl,
      AccountingPeriodUtil.getCurrentTaxYear.taxEndYear
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        accountingPeriod <- subscriptionDetailsService.fetchAccountingPeriodDate()
      } yield
        Ok(view(
          AccountingPeriodDateForm.accountingPeriodDateForm.fill(accountingPeriod),
          backUrl = backUrl(isEditMode),
          isEditMode = isEditMode
        ))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingPeriodDateForm.accountingPeriodDateForm.bindFromRequest().fold(
        formWithErrors => BadRequest(view(
          form = formWithErrors,
          backUrl = backUrl(isEditMode),
          isEditMode = isEditMode
        )),
        accountingPeriod =>
          subscriptionDetailsService.fetchIncomeSource() flatMap { incomeSources =>
            if (accountingPeriodService.checkEligibleAccountingPeriod(accountingPeriod.startDate.toLocalDate,
              accountingPeriod.endDate.toLocalDate, incomeSources.contains(IncomeSourceModel(true, true, false)))) {
              for {
                cache <- subscriptionDetailsService.fetchAll() map (_.get)
                _ = cache.getIncomeSource
                _ <- subscriptionDetailsService.saveAccountingPeriodDate(accountingPeriod)
              } yield {
                if (isEditMode) {
                  Redirect(controllers.agent.routes.CheckYourAnswersController.show())
                } else {
                  Redirect(controllers.agent.business.routes.BusinessAccountingMethodController.show())
                }
              }
            } else {
              Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show())
            }
          }
      )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      controllers.agent.business.routes.MatchTaxYearController.show().url
    }

  }
}
