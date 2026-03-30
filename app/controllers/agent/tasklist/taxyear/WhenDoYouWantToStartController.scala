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
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccess
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.AccountingYearForm
import models.{AccountingYear, Current}
import models.common.AccountingYearModel
import models.requests.agent.ConfirmedClientRequest
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.{AccountingPeriodService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.taxyear.WhenDoYouWantToStart

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhenDoYouWantToStartController @Inject()(whenDoYouWantToStart: WhenDoYouWantToStart,
                                               identify: IdentifierAction,
                                               journeyRefiner: ConfirmedClientJourneyRefiner,
                                               subscriptionDetailsService: SubscriptionDetailsService,
                                               accountingPeriodService: AccountingPeriodService)
                                              (val appConfig: AppConfig)
                                              (implicit mcc: MessagesControllerComponents,
                                               ec: ExecutionContext) extends SignUpBaseController {

  def view(accountingYearForm: Form[AccountingYear], isEditMode: Boolean)
          (implicit request: ConfirmedClientRequest[_]): Html =
    whenDoYouWantToStart(
      accountingYearForm = accountingYearForm,
      postAction = controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.submit(editMode = isEditMode),
      clientName = request.clientDetails.name,
      clientNino = request.clientDetails.formattedNino,
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )

  def show(isEditMode: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchSelectedTaxYear(request.reference) map {
      case Some(taxYearModel) if !taxYearModel.editable =>
        Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
      case accountingYearModel =>
        Ok(view(
          accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYearModel.map(aym => aym.accountingYear)),
          isEditMode = isEditMode
        ))
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    AccountingYearForm.accountingYearForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          accountingYearForm = formWithErrors,
          isEditMode = isEditMode
        ))),
      accountingYear => {
        saveSelectedTaxYear(accountingYear) flatMap { _ =>
          if (isEditMode) {
            Future.successful(Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show))
          } else {
            Future.successful(Redirect(controllers.agent.routes.WhatYouNeedToDoController.show()))
          }
        }
      }
    )
  }

  private def saveSelectedTaxYear(accountingYear: AccountingYear)
                                 (implicit request: ConfirmedClientRequest[_]): Future[PostSubscriptionDetailsSuccess] = {
    subscriptionDetailsService.saveSelectedTaxYear(request.reference, AccountingYearModel(accountingYear)) map {
      case Right(response) => response
      case Left(_) => throw new InternalServerException("[WhenDoYouWantToStartController][saveSelectedTaxYear] - Could not save accounting year")
    }
  }
}