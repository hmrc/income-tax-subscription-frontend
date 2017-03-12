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

package controllers

import javax.inject.Inject

import config.BaseControllerConfig
import forms.{IncomeSourceForm, IncomeTypeForm, OtherIncomeForm}
import models.OtherIncomeModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import utils.Implicits._

import scala.concurrent.Future

class OtherIncomeController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService
                                     ) extends BaseController {

  def view(otherIncomeForm: Form[OtherIncomeModel], backUrl: String)(implicit request: Request[_]): Html =
    views.html.other_income(
      otherIncomeForm = otherIncomeForm,
      postAction = controllers.routes.OtherIncomeController.submitOtherIncome(),
      backUrl = backUrl
    )

  val showOtherIncome: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        choice <- keystoreService.fetchOtherIncome()
      } yield Ok(view(OtherIncomeForm.otherIncomeForm.fill(choice), backUrl))
  }

  val submitOtherIncome: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      OtherIncomeForm.otherIncomeForm.bindFromRequest.fold(
        formWithErrors => BadRequest(view(otherIncomeForm = formWithErrors, backUrl = backUrl)),
        choice =>
          keystoreService.saveOtherIncome(choice).flatMap { _ =>
            choice.choice match {
              case OtherIncomeForm.option_yes =>
                Redirect(controllers.routes.OtherIncomeErrorController.showOtherIncomeError())
              case OtherIncomeForm.option_no =>
                keystoreService.fetchIncomeSource() map {
                  case Some(incomeSource) => incomeSource.source match {
                    case IncomeSourceForm.option_business =>
                      Redirect(controllers.business.routes.CurrentFinancialPeriodPriorController.show())
                    case IncomeSourceForm.option_property =>
                      Redirect(controllers.routes.TermsController.showTerms())
                    case IncomeSourceForm.option_both =>
                      Redirect(controllers.business.routes.CurrentFinancialPeriodPriorController.show())
                  }
                }
            }
          }
      )
  }

  lazy val backUrl: String = controllers.routes.IncomeSourceController.showIncomeSource().url

}
