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

import core.auth.SignUpController
import core.config.{AppConfig, BaseControllerConfig}
import core.services.{AccountingPeriodService, AuthService, KeystoreService}
import incometax.business.forms.AccountingYearForm
import incometax.business.models.AccountingYearModel
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class WhatYearToSignUpController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val authService: AuthService,
                                           val appConfig: AppConfig,
                                           val accountingPeriodService: AccountingPeriodService
                                          ) extends SignUpController {

  def view(accountingYearForm: Form[AccountingYearModel], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    incometax.business.views.html.what_year_to_sign_up(
      accountingYearForm = accountingYearForm,
      postAction = controllers.individual.business.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchSelectedTaxYear() map { accountingYear =>
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
          Future.successful(keystoreService.saveSelectedTaxYear(accountingYear)) map { _ =>
            if (isEditMode) {
              Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
            }
            else {
              Redirect(controllers.individual.business.routes.BusinessAccountingMethodController.show())
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    else
      controllers.individual.business.routes.MatchTaxYearController.show().url
}
