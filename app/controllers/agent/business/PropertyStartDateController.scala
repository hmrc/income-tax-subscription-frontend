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
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.utils.ReferenceRetrieval
import forms.agent.PropertyStartDateForm
import forms.agent.PropertyStartDateForm.propertyStartDateForm
import models.DateModel
import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.agent.business.PropertyStartDate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyStartDateController @Inject()(val propertyStartDate: PropertyStartDate,
                                            val auditingService: AuditingService,
                                            val authService: AuthService,
                                            val subscriptionDetailsService: SubscriptionDetailsService,
                                            val languageUtils: LanguageUtils)
                                           (implicit val ec: ExecutionContext,
                                            val appConfig: AppConfig,
                                            mcc: MessagesControllerComponents) extends AuthenticatedController
  with ImplicitDateFormatter with FeatureSwitching with ReferenceRetrieval {

  private def isSaveAndRetrieve: Boolean = isEnabled(SaveAndRetrieve)

  def view(propertyStartDateForm: Form[DateModel], isEditMode: Boolean, incomeSourceModel: Option[IncomeSourceModel])
          (implicit request: Request[_]): Html = {
    propertyStartDate(
      propertyStartDateForm = propertyStartDateForm,
      postAction = controllers.agent.business.routes.PropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode, incomeSourceModel)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchPropertyStartDate(reference) flatMap { propertyStartDate =>
          if (isEnabled(SaveAndRetrieve)) {
            Future.successful(Ok(view(
              propertyStartDateForm = form.fill(propertyStartDate),
              isEditMode = isEditMode,
              incomeSourceModel = None
            )))
          } else {
            subscriptionDetailsService.fetchIncomeSource(reference) map {
              case Some(incomeSource) => Ok(view(
                propertyStartDateForm = form.fill(propertyStartDate),
                isEditMode = isEditMode,
                incomeSourceModel = Some(incomeSource)
              ))
              case None => Redirect(controllers.agent.routes.IncomeSourceController.show())
            }
          }
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        form.bindFromRequest.fold(
          formWithErrors => if(isEnabled(SaveAndRetrieve)) {
            Future.successful(BadRequest(view(
              propertyStartDateForm = formWithErrors, isEditMode = isEditMode, None
            )))
          } else {
            subscriptionDetailsService.fetchIncomeSource(reference) map {
              case Some(incomeSource) => BadRequest(view(
                propertyStartDateForm = formWithErrors, isEditMode = isEditMode, Some(incomeSource)
              ))
              case None => Redirect(controllers.agent.routes.IncomeSourceController.show())
            }
          },

          startDate =>
            subscriptionDetailsService.savePropertyStartDate(reference, startDate) flatMap { _ =>
              val redirectUrl: Call = (isEditMode, isSaveAndRetrieve) match {
                case (true, true) => controllers.agent.business.routes.PropertyCheckYourAnswersController.show(isEditMode)
                case (true, false) => controllers.agent.routes.CheckYourAnswersController.show
                case (false, _) => controllers.agent.business.routes.PropertyAccountingMethodController.show()
              }
              Future.successful(Redirect(redirectUrl))
            }

        )
      }
  }

  def backUrl(isEditMode: Boolean, maybeIncomeSourceModel: Option[IncomeSourceModel]): String =
    (isEditMode, isSaveAndRetrieve, maybeIncomeSourceModel) match {
      case (true, true, _) => controllers.agent.business.routes.PropertyCheckYourAnswersController.show(isEditMode).url
      case (false, true, _) => controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
      case (true, false, _) => controllers.agent.routes.CheckYourAnswersController.show.url
      case (false, false, Some(incomeSourceModel)) if incomeSourceModel.selfEmployment =>
        appConfig.incomeTaxSelfEmploymentsFrontendUrl + "client/details/business-accounting-method"
      case _ =>
        controllers.agent.routes.IncomeSourceController.show().url
    }

  def form(implicit request: Request[_]): Form[DateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate.toLongDate, PropertyStartDateForm.maxStartDate.toLongDate)
  }
}
