/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.agent.tasklist.taxyear

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.Current
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import models.requests.agent.ConfirmedClientRequest
import services.*
import views.html.agent.tasklist.taxyear.MandatoryBothSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MandatoryBothSignUpController @Inject()(mandatoryBothSignUp:MandatoryBothSignUp,
                                              identify: IdentifierAction,
                                              journeyRefiner: ConfirmedClientJourneyRefiner,
                                              accountingPeriodService: AccountingPeriodService)
                                             (val appConfig: AppConfig)
                                             (implicit mcc: MessagesControllerComponents,
                                               ec: ExecutionContext) extends SignUpBaseController {

  def view(isEditMode: Boolean)(implicit request: ConfirmedClientRequest[_]): Html =
    mandatoryBothSignUp(
      postAction = controllers.agent.tasklist.taxyear.routes.MandatoryBothSignUpController.submit(editMode = isEditMode),
      clientName = request.clientDetails.name,
      clientNino = request.clientDetails.formattedNino,
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )

  def show(isEditMode: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    Future.successful(Ok(view(isEditMode = isEditMode)))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    Future.successful(
      if (isEditMode) {
        Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
      } else {
        Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
      }
    )
  }
}
