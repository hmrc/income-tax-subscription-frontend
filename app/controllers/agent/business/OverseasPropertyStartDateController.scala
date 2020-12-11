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
import forms.agent.OverseasPropertyStartDateForm
import forms.agent.OverseasPropertyStartDateForm._
import javax.inject.{Inject, Singleton}
import models.common.{IncomeSourceModel, OverseasPropertyStartDateModel}
import play.api.data.Form
import play.api.libs.functional.~
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyStartDateController @Inject()(val authService: AuthService,
                                                    val subscriptionDetailsService: SubscriptionDetailsService,
                                                    val languageUtils: LanguageUtils
                                                          )(implicit val ec: ExecutionContext, appConfig: AppConfig,
                                                            mcc: MessagesControllerComponents)
  extends AuthenticatedController with ImplicitDateFormatter with RequireAnswer with FeatureSwitching {

  def view(overseasPropertyStartDateForm: Form[OverseasPropertyStartDateModel], isEditMode: Boolean, incomeSourceModel: IncomeSourceModel)
          (implicit request: Request[_]): Html = {
    views.html.agent.business.overseas_property_start_date(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      postAction = controllers.agent.business.routes.OverseasPropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode, incomeSourceModel)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(optOverseasPropertyStartDateAnswer and incomeSourceModelAnswer) {
        case overseasPropertyStartDate ~ incomeSource =>
          Future.successful(Ok(view(
            overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate),
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
              Future.successful(BadRequest(view(overseasPropertyStartDateForm = formWithErrors, isEditMode = isEditMode, incomeSourceModel)))
          },
        startDate =>
          subscriptionDetailsService.saveOverseasPropertyStartDate(startDate) flatMap { _ =>
            if (isEditMode) {
              Future.successful(Redirect(controllers.agent.routes.CheckYourAnswersController.show()))
            } else {
              Future.successful(Redirect(controllers.agent.business.routes.OverseasPropertyAccountingMethodController.submit()))
            }
          }

      )
  }

  def backUrl(isEditMode: Boolean, incomeSourceModel: IncomeSourceModel): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      (incomeSourceModel.selfEmployment, incomeSourceModel.ukProperty) match {
        case (_, true) => controllers.agent.business.routes.PropertyAccountingMethodController.show().url
        case (true, false) =>
          if (isEnabled(ReleaseFour)) appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
          else controllers.agent.business.routes.BusinessAccountingMethodController.show().url
        case _ =>
          if (isEnabled(ReleaseFour)) controllers.agent.business.routes.WhatYearToSignUpController.show().url
          else controllers.agent.routes.IncomeSourceController.show().url
      }
    }
  }


  def form(implicit request: Request[_]): Form[OverseasPropertyStartDateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate.toLongDate, OverseasPropertyStartDateForm.maxStartDate.toLongDate)
  }

}
