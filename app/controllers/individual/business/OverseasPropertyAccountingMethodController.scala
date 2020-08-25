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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import forms.individual.business._
import javax.inject.Inject
import models.common.OverseasAccountingMethodPropertyModel
import models.individual.incomesource.IncomeSourceModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataUtil.CacheMapUtil

import scala.concurrent.{ExecutionContext, Future}

class OverseasPropertyAccountingMethodController @Inject()(val authService: AuthService,
                                                           val subscriptionDetailsService: SubscriptionDetailsService)
                                                          (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                                           mcc: MessagesControllerComponents) extends SignUpController {


  def view(overseasPropertyAccountingMethodForm: Form[OverseasAccountingMethodPropertyModel], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {
    views.html.individual.incometax.business.overseas_property_accounting_method(
      overseasPropertyAccountingMethodForm = overseasPropertyAccountingMethodForm,
      postAction = controllers.individual.business.routes.OverseasPropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }


  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchOverseasPropertyAccountingMethod() flatMap { accountingMethodOverseasProperty =>
        Future.successful(Ok(view(overseasPropertyAccountingMethodForm =
          AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(accountingMethodOverseasProperty),
          isEditMode = isEditMode)))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(overseasPropertyAccountingMethodForm = formWithErrors, isEditMode = isEditMode))),
        overseasPropertyAccountingMethod => {
          subscriptionDetailsService.saveOverseasAccountingMethodProperty(overseasPropertyAccountingMethod) map { _ =>
            Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
          }
        }
      )
  }


  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): String = {
    if (isEditMode) {
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    } else {
      controllers.individual.business.routes.OverseasPropertyCommencementDateController.show().url
    }

  }
}