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

package controllers.individual.tasklist.taxyear

import auth.individual.SignUpController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.*
import views.html.individual.tasklist.taxyear.MandatoryBothSignUp
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MandatoryBothSignUpController @Inject()(mandatoryBothSignUp: MandatoryBothSignUp,
                                              accountingPeriodService: AccountingPeriodService,
                                              sessionDataService: SessionDataService)
                                             (val auditingService: AuditingService,
                                              val authService: AuthService,
                                              val appConfig: AppConfig)
                                             (implicit val ec: ExecutionContext,
                                              mcc: MessagesControllerComponents) extends SignUpController {

  def view(isEditMode: Boolean)(implicit request: Request[_]): Html = {
    mandatoryBothSignUp(
      postAction = controllers.individual.tasklist.taxyear.routes.MandatoryBothSignUpController.submit(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      Future.successful(Ok(view(isEditMode = isEditMode)))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      Future.successful(
        if (isEditMode) {
          Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
        } else {
          Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
        }
      )
  }
}
