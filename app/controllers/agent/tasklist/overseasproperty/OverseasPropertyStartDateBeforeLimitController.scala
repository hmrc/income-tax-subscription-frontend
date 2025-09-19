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

package controllers.agent.tasklist.overseasproperty

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.OverseasPropertyStartDateBeforeLimitForm.overseasPropertyStartDateBeforeLimitForm
import models.No
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.overseasproperty.OverseasPropertyStartDateBeforeLimit

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyStartDateBeforeLimitController @Inject()(identify: IdentifierAction,
                                                               journeyRefiner: ConfirmedClientJourneyRefiner,
                                                               subscriptionDetailsService: SubscriptionDetailsService,
                                                               view: OverseasPropertyStartDateBeforeLimit)
                                                              (val appConfig: AppConfig)
                                                              (implicit cc: MessagesControllerComponents, ec: ExecutionContext)
  extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchForeignPropertyStartDateBeforeLimit(request.reference) map { maybeStartDateBeforeLimit =>
      Ok(view(
        overseasPropertyStartDateBeforeLimitForm = overseasPropertyStartDateBeforeLimitForm.fill(maybeStartDateBeforeLimit),
        postAction = routes.OverseasPropertyStartDateBeforeLimitController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    overseasPropertyStartDateBeforeLimitForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          overseasPropertyStartDateBeforeLimitForm = formWithErrors,
          postAction = routes.OverseasPropertyStartDateBeforeLimitController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit),
          clientDetails = request.clientDetails
        ))),
      { startDateBeforeLimit =>
        subscriptionDetailsService.saveForeignPropertyStartDateBeforeLimit(
          reference = request.reference, startDateBeforeLimit
        ) map {
          case Right(_) =>
            if (startDateBeforeLimit == No) {
              Redirect(routes.OverseasPropertyStartDateController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
            } else {
              Redirect(routes.OverseasPropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
            }
          case Left(_) =>
            throw new InternalServerException("[OverseasPropertyStartDateBeforeLimitController] - Could not save foreign property start date before limit")
        }
      }
    )
  }

  def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}