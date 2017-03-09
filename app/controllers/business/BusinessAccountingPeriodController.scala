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
import forms._
import models.{AccountingPeriodModel, CurrentFinancialPeriodPriorModel}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.KeystoreService
import models.enums.{AccountingPeriodViewType, CurrentAccountingPeriodView, NextAccountingPeriodView}
import utils.Implicits._

import scala.concurrent.Future

class BusinessAccountingPeriodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService
                                                  ) extends BaseController {

  def view(form: Form[AccountingPeriodModel], backUrl: String, isEditMode: Boolean, viewType: AccountingPeriodViewType)(implicit request: Request[_]): Html =
    views.html.business.accounting_period(
      form,
      controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod(editMode = isEditMode),
      backUrl = backUrl,
      viewType = viewType
    )

  def showAccountingPeriod(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        accountingPeriod <- keystoreService.fetchAccountingPeriod()
        backUrl <- backUrl
        viewType <- whichView
      } yield
        Ok(view(
          AccountingPeriodForm.accountingPeriodForm.fill(accountingPeriod),
          backUrl = backUrl,
          isEditMode = isEditMode,
          viewType = viewType
        ))
  }

  def submitAccountingPeriod(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request => {
      whichView.flatMap {
        viewType =>
          AccountingPeriodForm.accountingPeriodForm.bindFromRequest().fold(
            formWithErrors => backUrl.map(backUrl => BadRequest(view(
              form = formWithErrors,
              backUrl = backUrl,
              isEditMode = isEditMode,
              viewType = viewType
            ))),
            accountingPeriod =>
              keystoreService.saveAccountingPeriod(accountingPeriod) map (_ =>
                if (isEditMode)
                  Redirect(controllers.routes.SummaryController.showSummary())
                else
                  Redirect(controllers.business.routes.BusinessNameController.showBusinessName())
                )
          )
      }
    }
  }

  def whichView(implicit request: Request[_]): Future[AccountingPeriodViewType] = {

    keystoreService.fetchCurrentFinancialPeriodPrior().flatMap {
      case Some(currentPeriodPrior) =>
        currentPeriodPrior.currentPeriodIsPrior match {
          case CurrentFinancialPeriodPriorForm.option_yes =>
            NextAccountingPeriodView
          case CurrentFinancialPeriodPriorForm.option_no =>
            CurrentAccountingPeriodView
        }
    }
  }

  def backUrl(implicit request: Request[_]): Future[String] = {

    keystoreService.fetchCurrentFinancialPeriodPrior() flatMap {
      case Some(currentPeriodPrior) => currentPeriodPrior.currentPeriodIsPrior match {
        case CurrentFinancialPeriodPriorForm.option_yes =>
          controllers.business.routes.RegisterNextAccountingPeriodController.show().url
        case CurrentFinancialPeriodPriorForm.option_no =>
          controllers.business.routes.CurrentFinancialPeriodPriorController.show().url
      }
    }
  }

}
