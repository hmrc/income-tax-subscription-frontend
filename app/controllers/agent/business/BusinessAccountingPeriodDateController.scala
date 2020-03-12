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

import agent.auth.AuthenticatedController
import agent.services.CacheUtil._
import core.config.AppConfig
import core.utils.Implicits._
import forms.agent.AccountingPeriodDateForm
import incometax.AccountingPeriodUtil
import javax.inject.{Inject, Singleton}
import models.individual.business.AccountingPeriodModel
import models.individual.subscription.{Both, IncomeSourceType}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.agent.KeystoreService
import services.{AccountingPeriodService, AuthService}

import scala.concurrent.ExecutionContext

@Singleton
class BusinessAccountingPeriodDateController @Inject()(val authService: AuthService,
                                                       val messagesApi: MessagesApi,
                                                       accountingPeriodService: AccountingPeriodService,
                                                       keystoreService: KeystoreService)
                                                      (implicit val ec: ExecutionContext, appConfig: AppConfig) extends AuthenticatedController {

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
        accountingPeriod <- keystoreService.fetchAccountingPeriodDate()
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
          keystoreService.fetchIncomeSource() flatMap { incomeSources =>
            if (accountingPeriodService.checkEligibleAccountingPeriod(accountingPeriod.startDate.toLocalDate,
              accountingPeriod.endDate.toLocalDate, incomeSources.contains(Both))) {
              for {
                cache <- keystoreService.fetchAll() map (_.get)
                _ = cache.getIncomeSource() map (source => IncomeSourceType(source.source))
                _ <- keystoreService.saveAccountingPeriodDate(accountingPeriod)
              } yield {
                if (isEditMode) {
                  Redirect(controllers.agent.routes.CheckYourAnswersController.show())
                } else {
                  Redirect(controllers.agent.business.routes.BusinessAccountingMethodController.show())
                }
              }
            } else {
              Redirect(controllers.agent.eligibility.routes.NotEligibleForIncomeTaxController.show())
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




