/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.agent.eligibility

import java.time.LocalDate

import auth.agent.StatelessController
import config.AppConfig
import forms.agent.SoleTraderForm.soleTraderForm
import javax.inject.{Inject, Singleton}
import models._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.agent.eligibility.are_you_a_sole_trader

import scala.concurrent.ExecutionContext

@Singleton
class SoleTraderController @Inject()(val authService: AuthService)
                                    (implicit appConfig: AppConfig,
                                     mcc: MessagesControllerComponents,
                                     override val languageUtils: LanguageUtils,
                                     val ec: ExecutionContext) extends StatelessController with I18nSupport with ImplicitDateFormatter {

  private def startDateLimit: LocalDate = LocalDate.now.minusYears(2)

  def backUrl: String = routes.OtherSourcesOfIncomeController.show().url

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(are_you_a_sole_trader(soleTraderForm(startDateLimit.toLongDate), routes.SoleTraderController.submit(), startDateLimit.toLongDate, backUrl))
  }

  def submit(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      soleTraderForm(startDateLimit.toLongDate).bindFromRequest.fold(
        formWithErrors => BadRequest(are_you_a_sole_trader(formWithErrors, routes.SoleTraderController.submit(), startDateLimit.toLongDate, backUrl)),
        {
          case Yes => Redirect(routes.CannotTakePartController.show())
          case No => Redirect(routes.PropertyTradingStartAfterController.show())
        }
      )
  }
}
