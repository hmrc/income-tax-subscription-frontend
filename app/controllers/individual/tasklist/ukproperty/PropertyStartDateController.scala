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

package controllers.individual.tasklist.ukproperty

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.PropertyStartDateForm
import forms.individual.business.PropertyStartDateForm._
import models.DateModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.individual.tasklist.ukproperty.PropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyStartDateController @Inject()(propertyStartDate: PropertyStartDate,
                                            subscriptionDetailsService: SubscriptionDetailsService,
                                            referenceRetrieval: ReferenceRetrieval)
                                           (val auditingService: AuditingService,
                                            val authService: AuthService,
                                            val appConfig: AppConfig,
                                            val languageUtils: LanguageUtils)
                                           (implicit val ec: ExecutionContext,
                                            mcc: MessagesControllerComponents) extends SignUpController with ImplicitDateFormatter {

  def view(propertyStartDateForm: Form[DateModel], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {
    propertyStartDate(
      propertyStartDateForm = propertyStartDateForm,
      postAction = routes.PropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        reference <- referenceRetrieval.getIndividualReference
        startDate <- subscriptionDetailsService.fetchPropertyStartDate(reference)
      } yield {
        Ok(view(
          propertyStartDateForm = form.fill(startDate),
          isEditMode = isEditMode
        ))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(view(propertyStartDateForm = formWithErrors, isEditMode = isEditMode)))
          },
          startDate =>
            subscriptionDetailsService.savePropertyStartDate(reference, startDate) map {
              case Right(_) =>
                if (isEditMode) {
                  Redirect(routes.PropertyCheckYourAnswersController.show(isEditMode))
                } else {
                  Redirect(routes.PropertyAccountingMethodController.show(isEditMode))
                }
              case Left(_) => throw new InternalServerException("[PropertyStartDateController][submit] - Could not save start date")
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      routes.PropertyCheckYourAnswersController.show(editMode = true).url
    } else {
      controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }


  def form(implicit request: Request[_]): Form[DateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate, PropertyStartDateForm.maxStartDate, d => d.toLongDate)
  }
}
