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

import javax.inject.{Inject, Singleton}

import audit.Logging
import auth.AuthenticatedController
import config.BaseControllerConfig
import forms.IncomeSourceForm
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{AuthService, KeystoreService}

import scala.concurrent.Future

@Singleton
class OtherIncomeErrorController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val logging: Logging,
                                           val authService: AuthService
) extends AuthenticatedController {

  val showOtherIncomeError = Action.async { implicit request =>
    Future.successful(Ok(views.html.other_income_error(postAction = controllers.routes.OtherIncomeErrorController.submitOtherIncomeError(), backUrl)))
  }

  val submitOtherIncomeError: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource() map {
        case Some(incomeSource) => incomeSource.source match {
          case IncomeSourceForm.option_business =>
            Redirect(controllers.business.routes.BusinessAccountingPeriodPriorController.show())
          case IncomeSourceForm.option_property =>
            Redirect(controllers.routes.TermsController.showTerms())
          case IncomeSourceForm.option_both =>
            Redirect(controllers.business.routes.BusinessAccountingPeriodPriorController.show())
        }
        case _ =>
          logging.info("Tried to submit 'other income error' when no data found in Keystore for 'income source'")
          InternalServerError
      }
  }

  lazy val backUrl: String = controllers.routes.OtherIncomeController.showOtherIncome().url


}
