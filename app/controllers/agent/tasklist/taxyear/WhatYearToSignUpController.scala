/*
 * Copyright 2023 HM Revenue & Customs
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
import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.AccountingYearForm
import models.AccountingYear
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.taxyear.WhatYearToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYearToSignUpController @Inject()(whatYearToSignUp: WhatYearToSignUp,
                                           identify: IdentifierAction,
                                           journeyRefiner: ConfirmedClientJourneyRefiner,
                                           subscriptionDetailsService: SubscriptionDetailsService,
                                           accountingPeriodService: AccountingPeriodService)
                                          (val appConfig: AppConfig)
                                          (implicit mcc: MessagesControllerComponents,
                                           ec: ExecutionContext) extends SignUpBaseController with FeatureSwitching {

  def view(accountingYearForm: Form[AccountingYear], clientName: String, clientNino: String, isEditMode: Boolean)(implicit request: Request[_]): Html =
    whatYearToSignUp(
      accountingYearForm = accountingYearForm,
      postAction = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      clientName = clientName,
      clientNino = clientNino,
      backUrl = backUrl(isEditMode),
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
          clientName = request.clientDetails.name,
          clientNino = request.clientDetails.formattedNino,
          isEditMode = isEditMode
        ))
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    AccountingYearForm.accountingYearForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          accountingYearForm = formWithErrors,
          clientName = request.clientDetails.name,
          clientNino = request.clientDetails.formattedNino,
          isEditMode = isEditMode
        ))),
      accountingYear => {
        subscriptionDetailsService.saveSelectedTaxYear(request.reference, AccountingYearModel(accountingYear)) map {
          case Right(_) =>
            if (isEditMode && isEnabled(PrePopulate)) {
              Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
            } else if (isEnabled(PrePopulate)) {
              Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
            } else {
              Redirect(controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show())
            }
          case Left(_) => throw new InternalServerException("[WhatYearToSignUpController][submit] - Could not save accounting year")
        }
      }
    )
  }

  def backUrl(isEditMode: Boolean): Option[String] = {
    if (isEditMode && isEnabled(PrePopulate)) {
      Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
    } else if (isEnabled(PrePopulate)) {
      Some(controllers.agent.routes.UsingSoftwareController.show.url)
    } else if (isEditMode) {
      Some(controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode = true).url)
    } else {
      Some(controllers.agent.tasklist.routes.TaskListController.show().url)
    }
  }

}
