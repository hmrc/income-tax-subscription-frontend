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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import forms.individual.business.PropertyStartDateForm
import forms.individual.business.PropertyStartDateForm._
import models.DateModel
import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.individual.incometax.business.PropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyStartDateController @Inject()(val auditingService: AuditingService,
                                            val authService: AuthService,
                                            val subscriptionDetailsService: SubscriptionDetailsService,
                                            val languageUtils: LanguageUtils,
                                            propertyStartDate: PropertyStartDate)
                                           (implicit val ec: ExecutionContext,
                                            val appConfig: AppConfig,
                                            mcc: MessagesControllerComponents) extends SignUpController
  with ImplicitDateFormatter with FeatureSwitching {

  def view(propertyStartDateForm: Form[DateModel], isEditMode: Boolean, incomeSourceModel: Option[IncomeSourceModel])
          (implicit request: Request[_]): Html = {
    propertyStartDate(
      propertyStartDateForm = propertyStartDateForm,
      postAction = controllers.individual.business.routes.PropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode, incomeSourceModel),
      isSaveAndRetrieve = isEnabled(SaveAndRetrieve)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchPropertyStartDate() flatMap { propertyStartDate =>
        if (isEnabled(SaveAndRetrieve)) {
          Future.successful(Ok(view(
            propertyStartDateForm = form.fill(propertyStartDate),
            isEditMode = isEditMode,
            incomeSourceModel = None
          )))
        } else {
          subscriptionDetailsService.fetchIncomeSource() map {
            case Some(incomeSource) => Ok(view(
              propertyStartDateForm = form.fill(propertyStartDate),
              isEditMode = isEditMode,
              incomeSourceModel = Some(incomeSource)
            ))
            case None => Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show())
          }
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      form.bindFromRequest.fold(
        formWithErrors => {
          if (isEnabled(SaveAndRetrieve)) {
            Future.successful(BadRequest(view(propertyStartDateForm = formWithErrors, isEditMode = isEditMode, None)))
          } else {
            subscriptionDetailsService.fetchIncomeSource() map {
              case Some(incomeSource) => BadRequest(view(propertyStartDateForm = formWithErrors, isEditMode = isEditMode, Some(incomeSource)))
              case None => Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show())
            }
          }
        },
        startDate =>
          subscriptionDetailsService.savePropertyStartDate(startDate) flatMap { _ =>
            val redirectUrl = if (isEditMode && !isEnabled(SaveAndRetrieve)) {
              controllers.individual.subscription.routes.CheckYourAnswersController.show()
            } else {
              controllers.individual.business.routes.PropertyAccountingMethodController.show()
            }

            Future.successful(Redirect(redirectUrl))
          }

      )
  }

  def backUrl(isEditMode: Boolean, maybeIncomeSourceModel: Option[IncomeSourceModel])(implicit hc: HeaderCarrier): String =
    (isEditMode, isEnabled(SaveAndRetrieve), maybeIncomeSourceModel) match {
      case (true, true, _) => controllers.individual.business.routes.TaskListController.show().url
      case (false, true, _) => controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
      case (true, false, _) => controllers.individual.subscription.routes.CheckYourAnswersController.show().url
      case (false, false, Some(incomeSourceModel)) if incomeSourceModel.selfEmployment =>
        appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
      case _ =>
        controllers.individual.incomesource.routes.IncomeSourceController.show().url
    }

  def form(implicit request: Request[_]): Form[DateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate.toLongDate, PropertyStartDateForm.maxStartDate.toLongDate)
  }
}
