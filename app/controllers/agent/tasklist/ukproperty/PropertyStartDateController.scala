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
import config.featureswitch.FeatureSwitching
import controllers.utils.ReferenceRetrieval
import forms.agent.PropertyStartDateForm
import forms.agent.PropertyStartDateForm.propertyStartDateForm
import models.DateModel
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.tasklist.ukproperty.PropertyStartDate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyStartDateController @Inject()(propertyStartDate: PropertyStartDate)
                                           (val auditingService: AuditingService,
                                            val authService: AuthService,
                                            val sessionDataService: SessionDataService,
                                            val appConfig: AppConfig,
                                            val subscriptionDetailsService: SubscriptionDetailsService,
                                            val languageUtils: LanguageUtils)
                                           (implicit val ec: ExecutionContext,
                                            mcc: MessagesControllerComponents) extends AuthenticatedController
  with ImplicitDateFormatter with ReferenceRetrieval with FeatureSwitching {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchPropertyStartDate(reference) map { propertyStartDate =>
          Ok(view(
            propertyStartDateForm = form.fill(propertyStartDate),
            isEditMode = isEditMode
          ))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
              propertyStartDateForm = formWithErrors, isEditMode = isEditMode
            ))),
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

  private def view(propertyStartDateForm: Form[DateModel], isEditMode: Boolean)
                  (implicit request: Request[AnyContent]): Html = {
    propertyStartDate(
      propertyStartDateForm = propertyStartDateForm,
      postAction = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      clientDetails = request.clientDetails
    )
  }
}
