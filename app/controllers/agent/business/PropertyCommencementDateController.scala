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
import controllers.utils.Answers.{incomeSourceModelAnswer, incomeSourceModelAnswerAgent, optPropertyCommencementDateAnswer}
import controllers.utils.RequireAnswer
import forms.agent.PropertyCommencementDateForm
import forms.agent.PropertyCommencementDateForm.propertyCommencementDateForm
import javax.inject.Inject
import models.common.{IncomeSourceModel, PropertyCommencementDateModel}
import play.api.data.Form
import play.api.libs.functional.~
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter

import scala.concurrent.{ExecutionContext, Future}

class PropertyCommencementDateController @Inject()(val authService: AuthService,
                                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val languageUtils: LanguageUtils)
                                                  (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends AuthenticatedController
                                                   with ImplicitDateFormatter with RequireAnswer with FeatureSwitching {

  def view(propertyCommencementDateForm: Form[PropertyCommencementDateModel], isEditMode: Boolean, incomeSourceModel: IncomeSourceModel)
          (implicit request: Request[_]): Html = {
    views.html.agent.business.property_commencement_date(
      propertyCommencementDateForm = propertyCommencementDateForm,
      postAction = controllers.agent.business.routes.PropertyCommencementDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode, incomeSourceModel)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(optPropertyCommencementDateAnswer and incomeSourceModelAnswerAgent) {
        case propertyCommencementDate ~ incomeSource =>
          Future.successful(Ok(view(
            propertyCommencementDateForm = form.fill(propertyCommencementDate),
            isEditMode = isEditMode,
            incomeSourceModel = incomeSource
          )))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      form.bindFromRequest.fold(
        formWithErrors =>
          require(incomeSourceModelAnswerAgent) {
            incomeSourceModel =>
              Future.successful(BadRequest(view(propertyCommencementDateForm = formWithErrors, isEditMode = isEditMode, incomeSourceModel)))
          },
        startDate =>
          subscriptionDetailsService.savePropertyCommencementDate(startDate) flatMap { _ =>
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

  def form(implicit request: Request[_]): Form[PropertyCommencementDateModel] = {
    propertyCommencementDateForm(PropertyCommencementDateForm.propertyStartDate.toLongDate)
  }
}
