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
import forms.individual.business.AccountingYearForm
import models.AccountingYear
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.*
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.taxyear.WhenDoYouWantToStart
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhenDoYouWantToStartController @Inject()(whenDoYouWantToStart: WhenDoYouWantToStart,
                                               accountingPeriodService: AccountingPeriodService,
                                               subscriptionDetailsService: SubscriptionDetailsService)
                                              (identify: IdentifierAction,
                                               refine: SignUpJourneyRefiner)
                                              (implicit val ec: ExecutionContext,
                                               mcc: MessagesControllerComponents) extends SignUpBaseController {

  def view(accountingYearForm: Form[AccountingYear], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    whenDoYouWantToStart(
      accountingYearForm = accountingYearForm,
      postAction = controllers.individual.tasklist.taxyear.routes.WhenDoYouWantToStartController.submit(editMode = isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = (identify andThen refine).async { implicit request =>
    subscriptionDetailsService.fetchSelectedTaxYear(request.reference) map {
      case Some(taxYearModel) if !taxYearModel.editable =>
        Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
      case accountingYearModel =>
        Ok(view(
          accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYearModel.map(_.accountingYear)),
          isEditMode = isEditMode
        ))
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = (identify andThen refine).async { implicit request =>
    AccountingYearForm.accountingYearForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
      accountingYear => {
        subscriptionDetailsService.saveSelectedTaxYear(request.reference, AccountingYearModel(accountingYear)) map {
          case Right(_) =>
            if (isEditMode) {
              Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
            } else {
              Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
            }
          case Left(_) =>
            throw new InternalServerException("[WhenDoYouWantToStartController][submit] - Could not save accounting year")
        }
      }
    )
  }
}
