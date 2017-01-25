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
import forms.{IncomeSourceForm, SoleTraderForm}
import models.SoleTraderModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future


class SoleTraderController @Inject()(val baseConfig: BaseControllerConfig,
                                     val messagesApi: MessagesApi,
                                     val keystoreService: KeystoreService
                                    ) extends BaseController {

  def view(soleTraderForm: Form[SoleTraderModel], backUrl: String)(implicit request: Request[_]): Html =
    views.html.business.sole_trader(
      soleTraderForm = soleTraderForm,
      postAction = controllers.business.routes.SoleTraderController.submitSoleTrader(),
      backUrl = backUrl
    )

  val showSoleTrader: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        soleTrader <- keystoreService.fetchSoleTrader()
        backUrl <- backUrl
      } yield Ok(view(soleTraderForm = SoleTraderForm.soleTraderForm.fill(soleTrader), backUrl = backUrl))
  }

  val submitSoleTrader: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      SoleTraderForm.soleTraderForm.bindFromRequest.fold(
        formWithErrors => backUrl.map(backUrl => BadRequest(view(soleTraderForm = formWithErrors, backUrl = backUrl))),
        soleTrader =>
          keystoreService.saveSoleTrader(soleTrader) flatMap { _ =>
            soleTrader.isSoleTrader match {
              case SoleTraderForm.option_yes => yes
              case SoleTraderForm.option_no => no
            }
          }
      )
  }

  def yes(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod()))

  def no(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(controllers.routes.NotEligibleController.showNotEligible()))

  def backUrl(implicit request: Request[_]): Future[String] =
    keystoreService.fetchIncomeSource() map {
      case Some(source) => source.source match {
        case IncomeSourceForm.option_business =>
          controllers.routes.IncomeSourceController.showIncomeSource().url
        case IncomeSourceForm.option_both =>
          controllers.property.routes.PropertyIncomeController.showPropertyIncome().url
      }
    }

}
