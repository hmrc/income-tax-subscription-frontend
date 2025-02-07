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

package controllers.individual.tasklist.overseasproperty

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.ForeignPropertyStartDateBeforeLimitForm
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.overseasproperty.ForeignPropertyStartDateBeforeLimit

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForeignPropertyStartDateBeforeLimitController @Inject()(subscriptionDetailsService: SubscriptionDetailsService,
                                                              referenceRetrieval: ReferenceRetrieval,
                                                              view: ForeignPropertyStartDateBeforeLimit,
                                                              val appConfig: AppConfig,
                                                              val authService: AuthService,
                                                              val auditingService: AuditingService)
                                                             (implicit mcc: MessagesControllerComponents,
                                                              val ec: ExecutionContext) extends SignUpController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        reference <- referenceRetrieval.getIndividualReference
        maybeForeignPropertyStartDateBeforeLimit <- subscriptionDetailsService.fetchForeignPropertyStartDateBeforeLimit(reference)
      } yield {
        Ok(view(
          startDateBeforeLimitForm = ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.fill(maybeForeignPropertyStartDateBeforeLimit),
          postAction = routes.ForeignPropertyStartDateBeforeLimitController.submit(isEditMode, isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit)
        ))
      }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(
          startDateBeforeLimitForm = formWithErrors,
          postAction = routes.ForeignPropertyStartDateBeforeLimitController.submit(isEditMode, isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit)
        ))), { answer =>
          for {
            reference <- referenceRetrieval.getIndividualReference
            saveResult <- subscriptionDetailsService.saveForeignPropertyStartDateBeforeLimit(reference, answer)
          } yield {
            saveResult match {
              case Left(_) =>
                throw new InternalServerException("[ForeignPropertyStartDateBeforeLimitController][submit] - Failure during save")
              case Right(_) =>
                answer match {
                  case Yes =>
                    if (isEditMode || isGlobalEdit) {
                      Redirect(routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
                    } else {
                      Redirect(routes.OverseasPropertyAccountingMethodController.show())
                    }
                  case No =>
                    Redirect(routes.ForeignPropertyStartDateController.show(isEditMode, isGlobalEdit))
                }
            }
          }
        }
      )
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.OverseasPropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit).url
    } else {
      controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}
