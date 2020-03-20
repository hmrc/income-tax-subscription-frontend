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

import auth.individual.JourneyState.RequestFunctions
import auth.individual.{Registration, SignUpController}
import config.AppConfig
import forms.individual.business.AccountingPeriodDateForm
import javax.inject.{Inject, Singleton}
import models.Yes
import models.individual.business.AccountingPeriodModel
import models.individual.business.enums.{AccountingPeriodViewType, RegistrationAccountingPeriodView, SignUpAccountingPeriodView}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.individual.KeystoreService
import services.{AccountingPeriodService, AuthService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessAccountingPeriodDateController @Inject()(val authService: AuthService,
                                                       val messagesApi: MessagesApi,
                                                       accountingPeriodService: AccountingPeriodService,
                                                       keystoreService: KeystoreService)
                                                      (implicit val ec: ExecutionContext, appConfig: AppConfig) extends SignUpController {

  def view(form: Form[AccountingPeriodModel], backUrl: String, isEditMode: Boolean, editMatch: Boolean)(implicit request: Request[_]): Html =
    views.html.individual.incometax.business.accounting_period_date(
      accountingPeriodForm = form,
      postAction = controllers.individual.business.routes.BusinessAccountingPeriodDateController.submit(editMode = isEditMode, editMatch = editMatch),
      viewType = whichView,
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def show(isEditMode: Boolean, editMatch: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        accountingPeriod <- keystoreService.fetchAccountingPeriodDate()
      } yield
        Ok(view(
          AccountingPeriodDateForm.accountingPeriodDateForm.fill(accountingPeriod),
          backUrl = backUrl(isEditMode, editMatch),
          isEditMode = isEditMode,
          editMatch = editMatch
        ))
  }

  def submit(isEditMode: Boolean, editMatch: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingPeriodDateForm.accountingPeriodDateForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(
          form = formWithErrors,
          backUrl = backUrl(isEditMode, editMatch),
          isEditMode = isEditMode,
          editMatch = editMatch
        ))),
        accountingPeriod => {
          keystoreService.fetchRentUkProperty() flatMap { ukProperty =>
            if (accountingPeriodService.checkEligibleAccountingPeriod(
              accountingPeriod.startDate.toLocalDate,
              accountingPeriod.endDate.toLocalDate,
              ukProperty.exists(_.rentUkProperty == Yes)
            )) {
              keystoreService.saveAccountingPeriodDate(accountingPeriod) map { _ =>
                if (isEditMode) {
                  Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
                } else {
                  Redirect(controllers.individual.business.routes.BusinessAccountingMethodController.show())
                }
              }
            } else {
              Future.successful(Redirect(controllers.individual.eligibility.routes.NotEligibleForIncomeTaxController.show()))
            }
          }
        }
      )
  }

  def whichView(implicit request: Request[_]): AccountingPeriodViewType =
    if (request.isInState(Registration)) RegistrationAccountingPeriodView
    else SignUpAccountingPeriodView

  def backUrl(isEditMode: Boolean, editMatch: Boolean)(implicit request: Request[_]): String =
    if (isEditMode) {
      if (editMatch) {
        controllers.individual.business.routes.MatchTaxYearController.show(editMode = isEditMode).url
      } else {
        controllers.individual.subscription.routes.CheckYourAnswersController.show().url
      }
    } else {
      controllers.individual.business.routes.MatchTaxYearController.show(editMode = isEditMode).url
    }

}
