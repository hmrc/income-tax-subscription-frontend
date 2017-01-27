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
import forms.{AccountingPeriodForm, IncomeSourceForm, PropertyIncomeForm, SoleTraderForm}
import models.AccountingPeriodModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

class BusinessAccountingPeriodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService
                                                  ) extends BaseController {

  def view(form: Form[AccountingPeriodModel], backUrl: String)(implicit request: Request[_]): Html =
    views.html.business.accounting_period(
      form,
      controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod(),
      backUrl = backUrl
    )

  val showAccountingPeriod: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        accountingPeriod <- keystoreService.fetchAccountingPeriod()
        backUrl <- backUrl
      } yield
        Ok(view(
          AccountingPeriodForm.accountingPeriodForm.fill(accountingPeriod),
          backUrl = backUrl
        ))
  }

  val submitAccountingPeriod: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      AccountingPeriodForm.accountingPeriodForm.bindFromRequest().fold(
        formWithErrors => backUrl.map(backUrl => BadRequest(view(form = formWithErrors, backUrl = backUrl))),
        accountingPeriod => {
          keystoreService.saveAccountingPeriod(accountingPeriod) map (
            _ => Redirect(controllers.business.routes.BusinessNameController.showBusinessName()))
        }
      )
  }

  def backUrl(implicit request: Request[_]): Future[String] = {
    lazy val checkSoleTrader = keystoreService.fetchSoleTrader().map {
      case Some(soleTrader) =>
        soleTrader.isSoleTrader match {
          case SoleTraderForm.option_yes =>
            controllers.business.routes.SoleTraderController.showSoleTrader().url
          case SoleTraderForm.option_no =>
            controllers.routes.NotEligibleController.showNotEligible().url
        }
    }

    lazy val checkPropertyIncome = keystoreService.fetchPropertyIncome() flatMap {
      case Some(propertyIncome) => propertyIncome.incomeValue match {
        case PropertyIncomeForm.option_LT10k =>
          Future.successful(controllers.routes.NotEligibleController.showNotEligible().url)
        case PropertyIncomeForm.option_GE10k =>
          checkSoleTrader
      }
    }

    keystoreService.fetchIncomeSource() flatMap {
      case Some(incomeSource) => incomeSource.source match {
        case IncomeSourceForm.option_business =>
          checkSoleTrader
        case IncomeSourceForm.option_both =>
          checkPropertyIncome
      }
    }
  }

}
