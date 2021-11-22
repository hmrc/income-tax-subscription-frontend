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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyStartDateController @Inject()(val auditingService: AuditingService,
                                            val authService: AuthService,
                                            val subscriptionDetailsService: SubscriptionDetailsService,
                                            val languageUtils: LanguageUtils)
                                           (implicit val ec: ExecutionContext,
                                            val appConfig: AppConfig,
                                            mcc: MessagesControllerComponents) extends AuthenticatedController
  with ImplicitDateFormatter with ReferenceRetrieval {

  def view(propertyStartDateForm: Form[DateModel], isEditMode: Boolean, incomeSourceModel: IncomeSourceModel)
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
      withAgentReference { reference =>
        subscriptionDetailsService.fetchPropertyStartDate(reference) flatMap { propertyStartDate =>
          subscriptionDetailsService.fetchIncomeSource(reference) map {
            case Some(incomeSource) => Ok(view(
              propertyStartDateForm = form.fill(propertyStartDate),
              isEditMode = isEditMode,
              incomeSourceModel = incomeSource
            ))
            case None => Redirect(controllers.agent.routes.IncomeSourceController.show())
          }
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        form.bindFromRequest.fold(
          formWithErrors =>
            subscriptionDetailsService.fetchIncomeSource(reference) map {
              case Some(incomeSource) => BadRequest(view(
                propertyStartDateForm = formWithErrors, isEditMode = isEditMode, incomeSource
              ))
              case None => Redirect(controllers.agent.routes.IncomeSourceController.show())
            },
          startDate =>
            subscriptionDetailsService.savePropertyStartDate(reference, startDate) flatMap { _ =>
              if (isEditMode) {
                Future.successful(Redirect(controllers.agent.routes.CheckYourAnswersController.show()))
              } else {
                Future.successful(Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show()))
              }
            }

        )
      }
  }

  def backUrl(isEditMode: Boolean, incomeSourceModel: IncomeSourceModel): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      incomeSourceModel match {
        case IncomeSourceModel(true, _, _) => appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/client/details/business-accounting-method"
        case _ => controllers.agent.routes.IncomeSourceController.show().url
      }
    }
  }

  def form(implicit request: Request[_]): Form[DateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate.toLongDate, PropertyStartDateForm.maxStartDate.toLongDate)
  }

}
