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
import forms.agent.PropertyTradingStartDateForm.propertyTradingStartDateForm
import javax.inject.{Inject, Singleton}
import models.{No, Yes}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.agent.eligibility.property_trading_after

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyTradingStartAfterController @Inject()(val authService: AuthService,
                                                    val languageUtils: LanguageUtils)
                                                   (implicit appConfig: AppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext) extends StatelessController with ImplicitDateFormatter {

  private def startDateLimit: LocalDate = LocalDate.now.minusYears(1)

  def backUrl: String = routes.SoleTraderController.show().url

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(property_trading_after(propertyTradingStartDateForm(startDateLimit.toLongDate),
        routes.PropertyTradingStartAfterController.submit(), startDateLimit.toLongDate, backUrl))
  }

  def submit(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      propertyTradingStartDateForm(startDateLimit.toLongDate).bindFromRequest.fold(
        formWithErrors => BadRequest(property_trading_after(
          formWithErrors, routes.PropertyTradingStartAfterController.submit(), startDateLimit.toLongDate, backUrl)), {
          case Yes => Redirect(routes.CannotTakePartController.show())
          case No => Redirect(routes.AccountingPeriodCheckController.show())
        }
      )
  }

}