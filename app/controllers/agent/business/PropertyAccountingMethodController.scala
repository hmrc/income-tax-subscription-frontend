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
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import controllers.utils.AgentAnswers._
import controllers.utils.OptionalAnswers._
import controllers.utils.RequireAnswer
import forms.agent.AccountingMethodPropertyForm
import javax.inject.{Inject, Singleton}
import models.common.{AccountingMethodPropertyModel, IncomeSourceModel}
import play.api.data.Form
import play.api.libs.functional.~
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(val authService: AuthService, val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                                   appConfig: AppConfig) extends AuthenticatedController with FeatureSwitching with RequireAnswer {

  def view(accountingMethodPropertyForm: Form[AccountingMethodPropertyModel], incomeSource: IncomeSourceModel, isEditMode: Boolean)
          (implicit request: Request[_]): Html = {
    views.html.agent.business.property_accounting_method(
      accountingMethodPropertyForm = accountingMethodPropertyForm,
      postAction = controllers.agent.business.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(incomeSource, isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(incomeSourceModelAnswer and optPropertyAccountingMethod) { case incomeSource ~ propertyAccountingMethod =>
        Future.successful(Ok(view(
          accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(propertyAccountingMethod),
          incomeSource = incomeSource,
          isEditMode = isEditMode
        )))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(incomeSourceModelAnswer) { incomeSource =>
        AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
              accountingMethodPropertyForm = formWithErrors,
              incomeSource = incomeSource,
              isEditMode = isEditMode
            ))),
          accountingMethodProperty => {
            subscriptionDetailsService.saveAccountingMethodProperty(accountingMethodProperty) map { _ =>
              if (isEditMode || !incomeSource.foreignProperty) {
                Redirect(controllers.agent.routes.CheckYourAnswersController.show())
              } else {
                Redirect(routes.OverseasPropertyCommencementDateController.show())
              }
            }
          }
        )
      }
  }

  def backUrl(incomeSource: IncomeSourceModel, isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else if (isEnabled(ReleaseFour)) {
      controllers.agent.business.routes.PropertyCommencementDateController.show().url
    } else {
      incomeSource match {
        case IncomeSourceModel(true, _, _) => controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        case _ => controllers.agent.routes.IncomeSourceController.show().url
      }
    }
  }

}
