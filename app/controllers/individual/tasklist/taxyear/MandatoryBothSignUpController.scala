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

import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.*
import views.html.individual.tasklist.taxyear.MandatoryBothSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MandatoryBothSignUpController @Inject()(mandatoryBothSignUp: MandatoryBothSignUp,
                                              accountingPeriodService: AccountingPeriodService)
                                             (identify: IdentifierAction,
                                              refine: SignUpJourneyRefiner)
                                             (implicit val ec: ExecutionContext,
                                              mcc: MessagesControllerComponents) extends SignUpBaseController {

  def view(implicit request: Request[_]): Html = {
    mandatoryBothSignUp(
      postAction = controllers.individual.tasklist.taxyear.routes.MandatoryBothSignUpController.submit,
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear
    )
  }

  def show: Action[AnyContent] = (identify andThen refine) { implicit request =>
    Ok(view)
  }

  def submit: Action[AnyContent] = (identify andThen refine) { implicit request =>
    Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
  }
}
