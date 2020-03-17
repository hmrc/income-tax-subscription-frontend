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
import controllers.utils.AgentAnswers._
import controllers.utils.AgentRequireAnswer
import core.config.AppConfig
import forms.agent.AccountingMethodForm
import javax.inject.{Inject, Singleton}
import models.agent.AccountingMethodModel
import models.individual.business.MatchTaxYearModel
import models.individual.subscription.{Both, Business, IncomeSourceType}
import models.{No, Yes}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.functional.~
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.AuthService
import services.agent.KeystoreService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessAccountingMethodController @Inject()(val authService: AuthService,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService)
                                                  (implicit val ec: ExecutionContext,
                                                   appConfig: AppConfig) extends AuthenticatedController with AgentRequireAnswer {

  def view(accountingMethodForm: Form[AccountingMethodModel], isEditMode: Boolean, backUrl: String)(implicit request: Request[_]): Html = {
    views.html.agent.business.accounting_method(
      accountingMethodForm = accountingMethodForm,
      postAction = controllers.agent.business.routes.BusinessAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(optAccountingMethodAnswer and incomeSourceTypeAnswer and matchTaxYearAnswer) { case optAccountingMethod ~ incomeSourceType ~ matchTaxYear =>
        Future.successful(Ok(view(
          accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(optAccountingMethod),
          isEditMode = isEditMode,
          backUrl = backUrl(isEditMode, incomeSourceType, matchTaxYear)
        )))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(incomeSourceTypeAnswer and matchTaxYearAnswer) { case incomeSourceType ~ matchTaxYear =>
        AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(
            accountingMethodForm = formWithErrors,
            isEditMode = isEditMode,
            backUrl = backUrl(isEditMode, incomeSourceType, matchTaxYear)
          ))),
          accountingMethod => {
            keystoreService.saveAccountingMethod(accountingMethod) map { _ =>
              if (isEditMode || incomeSourceType != Both) {
                Redirect(controllers.agent.routes.CheckYourAnswersController.show())
              } else {
                Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show())
              }
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean, incomeSourceType: IncomeSourceType, matchTaxYear: MatchTaxYearModel): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      (matchTaxYear.matchTaxYear, incomeSourceType) match {
        case (No, _) => controllers.agent.business.routes.BusinessAccountingPeriodDateController.show().url
        case (Yes, Business) => controllers.agent.business.routes.WhatYearToSignUpController.show().url
        case _ => controllers.agent.business.routes.MatchTaxYearController.show().url
      }
    }
  }
}
