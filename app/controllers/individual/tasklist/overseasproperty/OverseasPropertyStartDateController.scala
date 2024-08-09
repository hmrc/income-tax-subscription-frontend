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

package controllers.individual.tasklist.overseasproperty

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.OverseasPropertyStartDateForm
import forms.individual.business.OverseasPropertyStartDateForm._
import models.DateModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.individual.tasklist.overseasproperty.OverseasPropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyStartDateController @Inject()(overseasPropertyStartDateView: OverseasPropertyStartDate,
                                                    subscriptionDetailsService: SubscriptionDetailsService,
                                                    referenceRetrieval: ReferenceRetrieval)
                                                   (val auditingService: AuditingService,
                                                    val authService: AuthService,
                                                    val languageUtils: LanguageUtils,
                                                    val appConfig: AppConfig)
                                                   (implicit val ec: ExecutionContext,
                                                    mcc: MessagesControllerComponents)
  extends SignUpController with ImplicitDateFormatter {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ => {
      referenceRetrieval.getIndividualReference flatMap { reference =>
        subscriptionDetailsService.fetchOverseasPropertyStartDate(reference) map { overseasPropertyStartDate =>
          Ok(view(
            overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate),
            isEditMode = isEditMode)
          )
        }
      }
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(overseasPropertyStartDateForm = formWithErrors, isEditMode = isEditMode))),
          startDate =>
            subscriptionDetailsService.saveOverseasPropertyStartDate(reference, startDate) map {
              case Right(_) =>
                if (isEditMode) {
                  Redirect(routes.OverseasPropertyCheckYourAnswersController.show(isEditMode))
                } else {
                  Redirect(routes.OverseasPropertyAccountingMethodController.show())
                }
              case Left(_) => throw new InternalServerException("[OverseasPropertyStartDateController][submit] - Could not save start date")
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
    } else {
      controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

  private def view(overseasPropertyStartDateForm: Form[DateModel], isEditMode: Boolean)
                  (implicit request: Request[_]): Html = {
    overseasPropertyStartDateView(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      postAction = routes.OverseasPropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  private def form(implicit request: Request[_]): Form[DateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate, OverseasPropertyStartDateForm.maxStartDate, d => d.toLongDate)
  }
}
