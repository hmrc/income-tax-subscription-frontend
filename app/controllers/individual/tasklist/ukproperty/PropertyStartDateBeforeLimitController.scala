/*
 * Copyright 2025 HM Revenue & Customs
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
import forms.individual.business.PropertyStartDateBeforeLimitForm
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.ukproperty.PropertyStartDateBeforeLimit

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyStartDateBeforeLimitController @Inject()(subscriptionDetailsService: SubscriptionDetailsService,
                                                       referenceRetrieval: ReferenceRetrieval,
                                                       view: PropertyStartDateBeforeLimit,
                                                       val appConfig: AppConfig,
                                                       val authService: AuthService,
                                                       val auditingService: AuditingService)
                                                      (implicit mcc: MessagesControllerComponents,
                                                       val ec: ExecutionContext) extends SignUpController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        reference <- referenceRetrieval.getIndividualReference
        maybePropertyStartDateBeforeLimit <- subscriptionDetailsService.fetchPropertyStartDateBeforeLimit(reference)
      } yield {
        Ok(view(
          startDateBeforeLimitForm = PropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.fill(maybePropertyStartDateBeforeLimit),
          postAction = routes.PropertyStartDateBeforeLimitController.submit(isEditMode, isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit)
        ))
      }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      PropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(
          startDateBeforeLimitForm = formWithErrors,
          postAction = routes.PropertyStartDateBeforeLimitController.submit(isEditMode, isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit)
        ))), { answer =>
          for {
            reference <- referenceRetrieval.getIndividualReference
            saveResult <- subscriptionDetailsService.savePropertyStartDateBeforeLimit(reference, answer)
          } yield {
            saveResult match {
              case Left(_) =>
                throw new InternalServerException("[PropertyStartDateBeforeLimitController][submit] - Failure during save")
              case Right(_) =>
                answer match {
                  case Yes =>
                    if (isEditMode || isGlobalEdit) {
                      Redirect(routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
                    } else {
                      Redirect(routes.PropertyAccountingMethodController.show())
                    }
                  case No =>
                    Redirect(routes.PropertyStartDateController.show(isEditMode, isGlobalEdit))
                }
            }
          }
        }
      )
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.PropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit).url
    } else {
      controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}
