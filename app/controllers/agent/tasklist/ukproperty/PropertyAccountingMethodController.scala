/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent.tasklist.ukproperty

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.agent.AccountingMethodPropertyForm
import models.AccountingMethod
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.tasklist.ukproperty.PropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(propertyAccountingMethod: PropertyAccountingMethod,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents,
                                                   val appConfig: AppConfig) extends AuthenticatedController with ReferenceRetrieval {

  def view(accountingMethodForm: Form[AccountingMethod], isEditMode: Boolean)
          (implicit request: Request[AnyContent]): Html = {
    propertyAccountingMethod(
      accountingMethodForm = accountingMethodForm,
      postAction = controllers.agent.tasklist.ukproperty.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      clientDetails = request.clientDetails
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
        AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(
            accountingMethodForm = formWithErrors,
            isEditMode = isEditMode
          ))),
          accountingMethodProperty => {
            subscriptionDetailsService.saveAccountingMethodProperty(reference, accountingMethodProperty) map {
              case Right(_) => Redirect(controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isEditMode))
              case Left(_) => throw new InternalServerException("[PropertyAccountingMethodController][submit] - Could not save accounting method")
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      routes.PropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      routes.PropertyStartDateController.show().url
    }
  }

}
