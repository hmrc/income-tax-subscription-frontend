/*
 * Copyright 2022 HM Revenue & Customs
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
import config.featureswitch.FeatureSwitching
import controllers.utils.ReferenceRetrieval
import forms.agent.AccountingMethodPropertyForm
import models.AccountingMethod
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import views.html.agent.business.PropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(propertyAccountingMethod: PropertyAccountingMethod,
                                                    val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents,
                                                   val appConfig: AppConfig) extends AuthenticatedController with FeatureSwitching with ReferenceRetrieval {

  def view(accountingMethodForm: Form[AccountingMethod], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {

  propertyAccountingMethod (
    accountingMethodForm = accountingMethodForm,
      postAction = controllers.agent.business.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchAccountingMethodProperty(reference) map { accountingMethod =>
          Ok(view(
            accountingMethodForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(accountingMethod),
            isEditMode = isEditMode
          ))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchIncomeSource(reference) flatMap {
          case Some(incomeSource) => AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(view(
              accountingMethodForm = formWithErrors,
              isEditMode = isEditMode
            ))),
            accountingMethod =>
              subscriptionDetailsService.saveAccountingMethodProperty(reference, accountingMethod) map { _ =>
                if (isEditMode || !incomeSource.foreignProperty) {
                  Redirect(controllers.agent.routes.CheckYourAnswersController.show)
                } else {
                  Redirect(routes.OverseasPropertyStartDateController.show())
                }
              }
          )
        }
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show.url
    } else {
      controllers.agent.business.routes.PropertyStartDateController.show().url
    }
  }

}
