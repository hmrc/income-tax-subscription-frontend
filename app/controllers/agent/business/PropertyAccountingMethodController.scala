/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.agent.AccountingMethodPropertyForm
import models.AccountingMethod
import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents,
                                                   val appConfig: AppConfig) extends AuthenticatedController with FeatureSwitching {

  def view(accountingMethodPropertyForm: Form[AccountingMethod], incomeSource: IncomeSourceModel, isEditMode: Boolean)
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
      subscriptionDetailsService.fetchAccountingMethodProperty() flatMap { accountingMethod =>
        subscriptionDetailsService.fetchIncomeSource() map {
          case Some(incomeSource) => Ok(view(
            accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(accountingMethod),
            incomeSource = incomeSource,
            isEditMode = isEditMode
          ))
          case None => Redirect(controllers.agent.routes.IncomeSourceController.show())
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchIncomeSource() flatMap {
        case Some(incomeSource) => AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(
            accountingMethodPropertyForm = formWithErrors,
            incomeSource = incomeSource,
            isEditMode = isEditMode
          ))),
          accountingMethod =>
            subscriptionDetailsService.saveAccountingMethodProperty(accountingMethod) map { _ =>
              if (isEditMode || !incomeSource.foreignProperty) {
                Redirect(controllers.agent.routes.CheckYourAnswersController.show())
              } else {
                Redirect(routes.OverseasPropertyStartDateController.show())
              }
            }
        )
      }
  }

  def backUrl(incomeSource: IncomeSourceModel, isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else if (isEnabled(ReleaseFour)) {
      controllers.agent.business.routes.PropertyStartDateController.show().url
    } else {
      incomeSource match {
        case IncomeSourceModel(true, _, _) => controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        case _ => controllers.agent.routes.IncomeSourceController.show().url
      }
    }
  }

}
