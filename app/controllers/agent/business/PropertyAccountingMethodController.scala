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
import forms.agent.AccountingMethodPropertyForm
import javax.inject.{Inject, Singleton}
import models.common.AccountingMethodPropertyModel
import models.individual.subscription.{Both, UkProperty}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataUtil._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(val authService: AuthService, subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                                   appConfig: AppConfig) extends AuthenticatedController {

  def view(accountingMethodPropertyForm: Form[AccountingMethodPropertyModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] = {
    for {
      back <- backUrl(isEditMode)
    } yield
      views.html.agent.business.property_accounting_method(
        accountingMethodPropertyForm = accountingMethodPropertyForm,
        postAction = controllers.agent.business.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = back
      )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchAccountingMethodProperty() flatMap { accountingMethodProperty =>
        view(accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm
          .fill(accountingMethodProperty), isEditMode = isEditMode).map(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          view(accountingMethodPropertyForm = formWithErrors, isEditMode = isEditMode).map(view => BadRequest(view)),
        accountingMethodProperty => {
          subscriptionDetailsService.saveAccountingMethodProperty(accountingMethodProperty) map { _ =>
            Redirect(controllers.agent.routes.CheckYourAnswersController.show())
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): Future[String] =
    if (isEditMode)
      Future.successful(controllers.agent.routes.CheckYourAnswersController.show().url)
    else {
      subscriptionDetailsService.fetchAll() map {
        case cacheMap => cacheMap.agentGetIncomeSource match {
          case Some(UkProperty) => controllers.agent.routes.IncomeSourceController.show().url
          case Some(Both) => controllers.agent.business.routes.BusinessAccountingMethodController.show().url
          case _ => controllers.agent.routes.IncomeSourceController.show().url
        }
      }
    }

}
