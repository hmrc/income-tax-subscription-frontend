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

import javax.inject.{Inject, Singleton}

import config.BaseControllerConfig
import controllers.BaseController
import forms.CurrentFinancialPeriodPriorForm
import models.{CurrentFinancialPeriodPriorModel, OtherIncomeModel}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import utils.Implicits._

import scala.concurrent.Future

@Singleton
class CurrentFinancialPeriodPriorController @Inject()(val baseConfig: BaseControllerConfig,
                                                      val messagesApi: MessagesApi,
                                                      val keystoreService: KeystoreService
                                                     ) extends BaseController {

  def view(currentFinancialPeriodPriorForm: Form[CurrentFinancialPeriodPriorModel])(implicit request: Request[_]): Future[Html] =
    backUrl.map { backUrl =>
      views.html.business.current_financial_period_prior(
        currentFinancialPeriodPriorForm = currentFinancialPeriodPriorForm,
        postAction = controllers.business.routes.CurrentFinancialPeriodPriorController.submit(),
        backUrl = backUrl
      )
    }

  val show: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchCurrentFinancialPeriodPrior().flatMap { x =>
        view(CurrentFinancialPeriodPriorForm.currentFinancialPeriodPriorForm.fill(x)).flatMap(view => Ok(view))
      }
  }

  val submit: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      CurrentFinancialPeriodPriorForm.currentFinancialPeriodPriorForm.bindFromRequest.fold(
        formWithErrors => view(formWithErrors).flatMap(view => BadRequest(view)),
        currentFinancialPeriodPrior =>
          keystoreService.saveCurrentFinancialPeriodPrior(currentFinancialPeriodPrior) flatMap { _ =>
            currentFinancialPeriodPrior.currentPeriodIsPrior match {
              case CurrentFinancialPeriodPriorForm.option_yes => yes
              case CurrentFinancialPeriodPriorForm.option_no => no
            }
          }
      )
  }

  def yes(implicit request: Request[_]): Future[Result] = Redirect(controllers.business.routes.RegisterNextAccountingPeriodController.show())

  def no(implicit request: Request[_]): Future[Result] = Redirect(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod())

  def backUrl(implicit request: Request[_]): Future[String] = {
    import forms.OtherIncomeForm._
    keystoreService.fetchOtherIncome().map {
      case Some(OtherIncomeModel(`option_yes`)) => controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
      case Some(OtherIncomeModel(`option_no`)) => controllers.routes.OtherIncomeController.showOtherIncome().url
    }
  }

}
