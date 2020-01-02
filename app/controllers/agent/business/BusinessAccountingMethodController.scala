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
import agent.forms.AccountingMethodForm
import agent.models.AccountingMethodModel
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import core.models.No
import core.services.AuthService
import incometax.business.models.MatchTaxYearModel
import incometax.subscription.models.{Both, Business, IncomeSourceType}
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class BusinessAccountingMethodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService,
                                                   val authService: AuthService
                                                  ) extends AuthenticatedController with FeatureSwitching {

  def view(accountingMethodForm: Form[AccountingMethodModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] = {
    for {
      incomeSource <- keystoreService.fetchIncomeSource()
      matchTaxYear <- keystoreService.fetchMatchTaxYear()
    } yield {
      agent.views.html.business.accounting_method(
        accountingMethodForm = accountingMethodForm,
        postAction = controllers.agent.business.routes.BusinessAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = backUrl(isEditMode, incomeSource, matchTaxYear)
      )
    }
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        accountingMethod <- keystoreService.fetchAccountingMethod()
        view <- view(accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(accountingMethod), isEditMode = isEditMode)
      } yield {
        Ok(view)
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
        formWithErrors => view(accountingMethodForm = formWithErrors, isEditMode = isEditMode).map(BadRequest(_)),
        accountingMethod => {
          for {
            _ <- keystoreService.saveAccountingMethod(accountingMethod)
            incomeSource <- keystoreService.fetchIncomeSource()
          } yield {
            if (isEditMode) {
              Redirect(controllers.agent.routes.CheckYourAnswersController.show())
            } else if (isEnabled(AgentPropertyCashOrAccruals) && incomeSource.contains(Both)) {
              Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show())
            } else if (isEnabled(EligibilityPagesFeature)) {
              Redirect(controllers.agent.routes.CheckYourAnswersController.show())
            } else {
              Redirect(controllers.agent.routes.TermsController.show())
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean, incomeSourceType: Option[IncomeSourceType], matchTaxYear: Option[MatchTaxYearModel]): String = {

    (incomeSourceType, matchTaxYear, isEditMode) match {
      case (_, _, true) => controllers.agent.routes.CheckYourAnswersController.show().url
      case (_, Some(MatchTaxYearModel(No)), _) => controllers.agent.business.routes.BusinessAccountingPeriodDateController.show().url
      case (Some(Both), _, _) => controllers.agent.business.routes.MatchTaxYearController.show().url
      case (Some(Business), _, _) => controllers.agent.business.routes.WhatYearToSignUpController.show().url
      case _ => controllers.agent.routes.IncomeSourceController.show().url
    }

  }
}
