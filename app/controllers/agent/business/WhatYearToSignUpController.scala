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
import agent.services.KeystoreService
import core.config.featureswitch.FeatureSwitching
import core.config.{AppConfig, BaseControllerConfig}
import core.services.{AccountingPeriodService, AuthService}
import forms.agent.AccountingYearForm
import javax.inject.Inject
import models.agent.AccountingYearModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

class WhatYearToSignUpController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val authService: AuthService,
                                           val accountingPeriodService: AccountingPeriodService
                                          )(implicit val ec: ExecutionContext) extends AuthenticatedController with FeatureSwitching {

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      controllers.agent.business.routes.MatchTaxYearController.show().url
    }
  }

  def view(accountingYearForm: Form[AccountingYearModel], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    views.html.agent.business.what_year_to_sign_up(
      accountingYearForm = accountingYearForm,
      postAction = controllers.agent.business.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchWhatYearToSignUp() map { accountingYear =>
        Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYear),
          isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingYearForm.accountingYearForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
        accountingYear => {
          Future.successful(keystoreService.saveWhatYearToSignUp(accountingYear)) map { _ =>
            if (isEditMode) {
              Redirect(controllers.agent.routes.CheckYourAnswersController.show())
            } else {
              Redirect(controllers.agent.business.routes.BusinessAccountingMethodController.show())
            }
          }
        }
      )
  }
}
