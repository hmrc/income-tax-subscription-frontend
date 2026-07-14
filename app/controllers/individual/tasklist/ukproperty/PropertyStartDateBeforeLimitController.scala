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

import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import forms.individual.business.PropertyStartDateBeforeLimitForm
import models.requests.individual.SignUpRequest
import models.{No, Yes, YesNo}
import play.api.mvc.*
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.ukproperty.PropertyStartDateBeforeLimit

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyStartDateBeforeLimitController @Inject()(identify: IdentifierAction,
                                                       journeyRefiner: SignUpJourneyRefiner,
                                                       subscriptionDetailsService: SubscriptionDetailsService,
                                                       view: PropertyStartDateBeforeLimit)
                                                      (implicit ec: ExecutionContext,
                                                       mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchPropertyStartDateBeforeLimit(request.reference) map { maybePropertyStartDateBeforeLimit =>
      Ok(view(
        startDateBeforeLimitForm = PropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.fill(maybePropertyStartDateBeforeLimit),
        postAction = routes.PropertyStartDateBeforeLimitController.submit(isEditMode, isGlobalEdit)
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    PropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(
        startDateBeforeLimitForm = formWithErrors,
        postAction = routes.PropertyStartDateBeforeLimitController.submit(isEditMode, isGlobalEdit)
      ))),
      {
        case Yes =>
          saveAnswerAndRedirect(
            answer = Yes,
            call = routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit)
          )
        case No =>
          saveAnswerAndRedirect(
            answer = No,
            call = routes.PropertyStartDateController.show(isEditMode, isGlobalEdit)
          )
      }
    )
  }

  private def saveAnswerAndRedirect(answer: YesNo, call: Call)
                                   (implicit request: SignUpRequest[_]): Future[Result] = {
    subscriptionDetailsService.savePropertyStartDateBeforeLimit(request.reference, answer) map {
      case Right(_) => Redirect(call)
      case Left(_) => throw new InternalServerException("[PropertyStartDateBeforeLimitController][saveAnswerAndRedirect] - Failure during save")
    }
  }

}
