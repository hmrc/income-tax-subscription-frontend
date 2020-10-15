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
import controllers.utils.Answers._
import controllers.utils.RequireAnswer
import forms.agent.AccountingMethodForm
import javax.inject.{Inject, Singleton}
import models.common.{AccountingMethodModel, IncomeSourceModel}
import play.api.data.Form
import play.api.libs.functional.~
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessAccountingMethodController @Inject()(val authService: AuthService, val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                                   appConfig: AppConfig) extends AuthenticatedController with RequireAnswer {

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
      require(optAccountingMethodAnswer) {
        case optAccountingMethod =>
          Future.successful(Ok(view(
            accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(optAccountingMethod),
            isEditMode = isEditMode,
            backUrl = backUrl(isEditMode)
          )))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(incomeSourceModelAnswer) { incomeSourceModel =>
        AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(
            accountingMethodForm = formWithErrors,
            isEditMode = isEditMode,
            backUrl = backUrl(isEditMode)
          ))),
          accountingMethod => {
            subscriptionDetailsService.saveAccountingMethod(accountingMethod) map { _ =>
              if (isEditMode || incomeSourceModel.selfEmployment && !incomeSourceModel.ukProperty && !incomeSourceModel.foreignProperty) {
                Redirect(controllers.agent.routes.CheckYourAnswersController.show())
              } else {
                Redirect(controllers.agent.business.routes.PropertyCommencementDateController.show())
              }
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      controllers.agent.business.routes.BusinessNameController.show().url
    }
  }
}