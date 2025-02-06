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
import forms.individual.business.ForeignPropertyStartDateForm.startDateForm
import models.DateModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.individual.tasklist.overseasproperty.ForeignPropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForeignPropertyStartDateController @Inject()(view: ForeignPropertyStartDate,
                                                   subscriptionDetailsService: SubscriptionDetailsService,
                                                   referenceRetrieval: ReferenceRetrieval)
                                                  (val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val appConfig: AppConfig,
                                                   val languageUtils: LanguageUtils)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpController with ImplicitDateFormatter {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        reference <- referenceRetrieval.getIndividualReference
        startDate <- subscriptionDetailsService.fetchForeignPropertyStartDate(reference)
      } yield {
        Ok(view(
          startDateForm = form.fill(startDate),
          postAction = routes.ForeignPropertyStartDateController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit)
        ))
      }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(view(
              startDateForm = formWithErrors,
              postAction = routes.ForeignPropertyStartDateController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
              backUrl = backUrl(isEditMode, isGlobalEdit)
            )))
          },
          startDate =>
            subscriptionDetailsService.saveForeignPropertyStartDate(reference, startDate) map {
              case Right(_) =>
                if (isEditMode || isGlobalEdit) {
                  Redirect(routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
                } else {
                  Redirect(routes.OverseasPropertyAccountingMethodController.show())
                }
              case Left(_) => throw new InternalServerException("[ForeignPropertyStartDateController][submit] - Could not save start date")
            }
        )
      }
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    routes.ForeignPropertyStartDateBeforeLimitController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit).url
  }

  def form(implicit request: Request[_]): Form[DateModel] = {
    startDateForm(_.toLongDate)
  }
}
