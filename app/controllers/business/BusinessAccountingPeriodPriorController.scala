/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.business

import javax.inject.Inject

import config.BaseControllerConfig
import controllers.BaseController
import forms.AccountingPeriodPriorForm
import models.{AccountingPeriodPriorModel, OtherIncomeModel}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import utils.Implicits._

import scala.concurrent.Future


class BusinessAccountingPeriodPriorController @Inject()(val baseConfig: BaseControllerConfig,
                                                        val messagesApi: MessagesApi,
                                                        val keystoreService: KeystoreService
                                                       ) extends BaseController {

  def view(accountingPeriodPriorForm: Form[AccountingPeriodPriorModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] =
    backUrl.map { backUrl =>
      views.html.business.accounting_period_prior(
        accountingPeriodPriorForm = accountingPeriodPriorForm,
        postAction = controllers.business.routes.BusinessAccountingPeriodPriorController.submit(editMode = isEditMode),
        backUrl = backUrl,
        isEditMode = isEditMode
      )
    }

  def show(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchCurrentFinancialPeriodPrior().flatMap { x =>
        view(AccountingPeriodPriorForm.accountingPeriodPriorForm.fill(x), isEditMode = isEditMode).flatMap(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      AccountingPeriodPriorForm.accountingPeriodPriorForm.bindFromRequest.fold(
        formWithErrors => view(formWithErrors, isEditMode = isEditMode).flatMap(view => BadRequest(view)),
        currentFinancialPeriodPrior =>
          keystoreService.fetchCurrentFinancialPeriodPrior().flatMap {
            somePreviousAnswer =>
              keystoreService.saveCurrentFinancialPeriodPrior(currentFinancialPeriodPrior) flatMap { _ =>
                if (somePreviousAnswer.fold(false)(previousAnswer => previousAnswer.equals(currentFinancialPeriodPrior)) && isEditMode)
                  Redirect(controllers.routes.CheckYourAnswersController.show())
                else
                  currentFinancialPeriodPrior.currentPeriodIsPrior match {
                    case AccountingPeriodPriorForm.option_yes => yes
                    case AccountingPeriodPriorForm.option_no => no
                  }
              }
          }
      )
  }

  def yes(implicit request: Request[_]): Future[Result] = Redirect(controllers.business.routes.RegisterNextAccountingPeriodController.show())

  def no(implicit request: Request[_]): Future[Result] = Redirect(controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod())

  def backUrl(implicit request: Request[_]): Future[String] = {
    import forms.OtherIncomeForm._
    keystoreService.fetchOtherIncome().map {
      case Some(OtherIncomeModel(`option_yes`)) => controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
      case Some(OtherIncomeModel(`option_no`)) => controllers.routes.OtherIncomeController.showOtherIncome().url
    }
  }

}
