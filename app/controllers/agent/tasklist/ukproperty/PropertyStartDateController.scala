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
import forms.agent.PropertyStartDateForm
import forms.agent.PropertyStartDateForm.propertyStartDateForm
import models.DateModel
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import utilities.UserMatchingSessionUtil.ClientDetails
import views.html.agent.tasklist.ukproperty.PropertyStartDate

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PropertyStartDateController @Inject()(propertyStartDate: PropertyStartDate,
                                            subscriptionDetailsService: SubscriptionDetailsService,
                                            clientDetailsRetrieval: ClientDetailsRetrieval,
                                            referenceRetrieval: ReferenceRetrieval)
                                           (val auditingService: AuditingService,
                                            val authService: AuthService,
                                            val appConfig: AppConfig,
                                            val languageUtils: LanguageUtils)
                                           (implicit val ec: ExecutionContext,
                                            mcc: MessagesControllerComponents) extends AuthenticatedController with ImplicitDateFormatter {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        reference <- referenceRetrieval.getAgentReference
        startDate <- subscriptionDetailsService.fetchPropertyStartDate(reference)
        clientDetails <- clientDetailsRetrieval.getClientDetails
      } yield {
        Ok(view(
          propertyStartDateForm = form.fill(startDate),
          isEditMode = isEditMode,
          clientDetails = clientDetails
        ))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        form.bindFromRequest().fold(
          formWithErrors =>
            clientDetailsRetrieval.getClientDetails map { clientDetails =>
              BadRequest(view(
                propertyStartDateForm = formWithErrors, isEditMode = isEditMode, clientDetails = clientDetails
              ))
            },
          startDate =>
            subscriptionDetailsService.savePropertyStartDate(reference, startDate) map {
              case Right(_) =>
                if (isEditMode) {
                  Redirect(routes.PropertyCheckYourAnswersController.show(isEditMode))
                } else {
                  Redirect(routes.PropertyAccountingMethodController.show())
                }
              case Left(_) => throw new InternalServerException("[PropertyStartDateController][submit] - Could not save start date")
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      routes.PropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

  private def form(implicit request: Request[_]): Form[DateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate, PropertyStartDateForm.maxStartDate, d => d.toLongDate)
  }

  private def view(propertyStartDateForm: Form[DateModel], isEditMode: Boolean, clientDetails: ClientDetails)
                  (implicit request: Request[AnyContent]): Html = {
    propertyStartDate(
      propertyStartDateForm = propertyStartDateForm,
      postAction = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      clientDetails = clientDetails
    )
  }
}
