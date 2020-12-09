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
import forms.agent.AccountingMethodOverseasPropertyForm
import javax.inject.{Inject, Singleton}
import models.common.OverseasAccountingMethodPropertyModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyAccountingMethodController @Inject()(val authService: AuthService,
                                                           val subscriptionDetailsService: SubscriptionDetailsService)
                                                          (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                                           mcc: MessagesControllerComponents) extends AuthenticatedController {

  def view(accountingMethodOverseasPropertyForm: Form[OverseasAccountingMethodPropertyModel], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {
    views.html.agent.business.overseas_property_accounting_method(
      accountingMethodOverseasPropertyForm = accountingMethodOverseasPropertyForm,
      postAction = controllers.agent.business.routes.OverseasPropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchOverseasPropertyAccountingMethod() flatMap { accountingMethodOverseasProperty =>
        Future.successful(Ok(view(accountingMethodOverseasPropertyForm =
          AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(accountingMethodOverseasProperty),
          isEditMode = isEditMode)))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(accountingMethodOverseasPropertyForm = formWithErrors, isEditMode = isEditMode))),
        overseasPropertyAccountingMethod => {
          subscriptionDetailsService.saveOverseasAccountingMethodProperty(overseasPropertyAccountingMethod) map { _ =>
            Redirect(controllers.agent.routes.CheckYourAnswersController.show())
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      controllers.agent.business.routes.OverseasPropertyStartDateController.show().url
    }
  }
}
