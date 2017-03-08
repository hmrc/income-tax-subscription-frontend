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
import forms.AccountingPeriodForm
import models.AccountingPeriodModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.KeystoreService
import utils.Implicits._

class BusinessAccountingPeriodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService
                                                  ) extends BaseController {

  def view(form: Form[AccountingPeriodModel], backUrl: String, isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.business.accounting_period(
      form,
      controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod(editMode = isEditMode),
      backUrl = backUrl
    )

  def showAccountingPeriod(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        accountingPeriod <- keystoreService.fetchAccountingPeriod()
      } yield
        Ok(view(
          AccountingPeriodForm.accountingPeriodForm.fill(accountingPeriod),
          backUrl,
          isEditMode = isEditMode
        ))
  }

  def submitAccountingPeriod(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      AccountingPeriodForm.accountingPeriodForm.bindFromRequest().fold(
        formWithErrors => BadRequest(view(form = formWithErrors, backUrl = backUrl, isEditMode = isEditMode)),
        accountingPeriod =>
          keystoreService.saveAccountingPeriod(accountingPeriod) map (_ =>
            if (isEditMode)
              Redirect(controllers.routes.SummaryController.showSummary())
            else
              Redirect(controllers.business.routes.BusinessNameController.showBusinessName())
            )
      )
  }

  lazy val backUrl: String = controllers.routes.IncomeSourceController.showIncomeSource().url
}
