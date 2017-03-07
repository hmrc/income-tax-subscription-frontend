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
import forms.CurrentFinancialPeriodPriorForm
import models.CurrentFinancialPeriodPriorModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import utils.Implicits._

import scala.concurrent.Future


class CurrentFinancialPeriodPriorController @Inject()(val baseConfig: BaseControllerConfig,
                                                      val messagesApi: MessagesApi,
                                                      val keystoreService: KeystoreService
                                    ) extends BaseController {

  def view(currentFinancialPeriodPriorForm: Form[CurrentFinancialPeriodPriorModel])(implicit request: Request[_]): Html =
    views.html.business.current_financial_period_prior(
      currentFinancialPeriodPriorForm = currentFinancialPeriodPriorForm,
      postAction = controllers.business.routes.CurrentFinancialPeriodPriorController.submit(),
      backUrl = controllers.routes.OtherIncomeController.showOtherIncome
    )

  val show: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchCurrentFinancialPeriodPrior().map { x =>
        Ok(view(CurrentFinancialPeriodPriorForm.currentFinancialPeriodPriorForm.fill(x)))
      }
  }

  val submit: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      CurrentFinancialPeriodPriorForm.currentFinancialPeriodPriorForm.bindFromRequest.fold(
        formWithErrors => BadRequest(view(formWithErrors)),
        currentFinancialPeriodPrior =>
          keystoreService.saveCurrentFinancialPeriodPrior(currentFinancialPeriodPrior) flatMap { _ =>
            currentFinancialPeriodPrior.currentPeriodIsPrior match {
              case CurrentFinancialPeriodPriorForm.option_yes => yes
              case CurrentFinancialPeriodPriorForm.option_no => no
            }
          }
      )
  }

  def yes(implicit request: Request[_]): Future[Result] = NotImplemented //TODO: Needs to go to 'Next Accounting Period' Question page

  def no(implicit request: Request[_]): Future[Result] = Redirect(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod())
}
