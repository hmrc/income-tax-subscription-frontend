/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.UkPropertyStartDateBeforeLimitForm.ukPropertyStartDateBeforeLimitForm
import models.No
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.ukproperty.PropertyStartDateBeforeLimit

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyStartDateBeforeLimitController @Inject()(identify: IdentifierAction,
                                                       journeyRefiner: ConfirmedClientJourneyRefiner,
                                                       subscriptionDetailsService: SubscriptionDetailsService,
                                                       view: PropertyStartDateBeforeLimit)
                                                      (val appConfig: AppConfig)
                                                      (implicit cc: MessagesControllerComponents, ec: ExecutionContext)
  extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchPropertyStartDateBeforeLimit(request.reference) map { maybeStartDateBeforeLimit =>
      Ok(view(
        ukPropertyStartDateBeforeLimitForm = ukPropertyStartDateBeforeLimitForm.fill(maybeStartDateBeforeLimit),
        postAction = routes.PropertyStartDateBeforeLimitController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    ukPropertyStartDateBeforeLimitForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          ukPropertyStartDateBeforeLimitForm = formWithErrors,
          postAction = routes.PropertyStartDateBeforeLimitController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit),
          clientDetails = request.clientDetails
        ))),
      { startDateBeforeLimit =>
        subscriptionDetailsService.savePropertyStartDateBeforeLimit(
          reference = request.reference, startDateBeforeLimit
        ) map {
          case Right(_) =>
            if (startDateBeforeLimit == No) {
              Redirect(routes.PropertyStartDateController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
            } else {
              Redirect(routes.PropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
            }
          case Left(_) =>
            throw new InternalServerException("[PropertyStartDateBeforeLimitController] - Could not save uk property start date before limit")
        }
      }
    )
  }

  def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}