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
import controllers.utils.AgentAnswers._
import controllers.utils.OptionalAnswers._
import controllers.utils.RequireAnswer
import forms.agent.PropertyStartDateForm
import forms.agent.PropertyStartDateForm.propertyStartDateForm
import javax.inject.Inject
import models.common.{IncomeSourceModel, PropertyStartDateModel}
import play.api.data.Form
import play.api.libs.functional.~
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter

import scala.concurrent.{ExecutionContext, Future}

class PropertyStartDateController @Inject()(val auditingService: AuditingService,
                                            val authService: AuthService,
                                            val subscriptionDetailsService: SubscriptionDetailsService,
                                            val languageUtils: LanguageUtils)
                                           (implicit val ec: ExecutionContext,
                                            val appConfig: AppConfig,
                                            mcc: MessagesControllerComponents) extends AuthenticatedController
  with ImplicitDateFormatter with RequireAnswer with FeatureSwitching {

  def view(propertyStartDateForm: Form[PropertyStartDateModel], isEditMode: Boolean, incomeSourceModel: IncomeSourceModel)
          (implicit request: Request[_]): Html = {
    views.html.agent.business.property_start_date(
      propertyStartDateForm = propertyStartDateForm,
      postAction = controllers.agent.business.routes.PropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode, incomeSourceModel)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(optPropertyStartDateAnswer and incomeSourceModelAnswer) {
        case propertyStartDate ~ incomeSource =>
          Future.successful(Ok(view(
            propertyStartDateForm = form.fill(propertyStartDate),
            isEditMode = isEditMode,
            incomeSourceModel = incomeSource
          )))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      form.bindFromRequest.fold(
        formWithErrors =>
          require(incomeSourceModelAnswer) {
            incomeSourceModel =>
              Future.successful(BadRequest(view(propertyStartDateForm = formWithErrors, isEditMode = isEditMode, incomeSourceModel)))
          },
        startDate =>
          subscriptionDetailsService.savePropertyStartDate(startDate) flatMap { _ =>
            if (isEditMode) {
              Future.successful(Redirect(controllers.agent.routes.CheckYourAnswersController.show()))
            } else {
              Future.successful(Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show()))
            }
          }

      )
  }

  def backUrl(isEditMode: Boolean, incomeSourceModel: IncomeSourceModel): String = {
    if (isEditMode) controllers.agent.routes.CheckYourAnswersController.show().url
    else
      incomeSourceModel match {
        case IncomeSourceModel(true, _, _) =>
          if (isEnabled(ReleaseFour)) controllers.agent.business.routes.BusinessAccountingMethodController.show().url
          else controllers.agent.routes.IncomeSourceController.show().url
        case _ => controllers.agent.routes.IncomeSourceController.show().url
      }
  }

  def form(implicit request: Request[_]): Form[PropertyStartDateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate.toLongDate, PropertyStartDateForm.maxStartDate.toLongDate)
  }
}
