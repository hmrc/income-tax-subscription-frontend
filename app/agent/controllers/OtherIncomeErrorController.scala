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

package agent.controllers

import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.auth.AuthenticatedController
import core.config.BaseControllerConfig
import agent.forms.IncomeSourceForm
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import agent.services.KeystoreService
import core.services.AuthService
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class OtherIncomeErrorController @Inject()(implicit val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val authService: AuthService,
                                           val logging: Logging
                                          ) extends AuthenticatedController {

  val showOtherIncomeError = Action.async { implicit request =>
    Future.successful(Ok(agent.views.html.other_income_error(postAction = agent.controllers.routes.OtherIncomeErrorController.submitOtherIncomeError(), backUrl)))
  }

  val submitOtherIncomeError: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource() map {
        case Some(incomeSource) => incomeSource.source match {
          case IncomeSourceForm.option_business =>
            Redirect(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show())
          case IncomeSourceForm.option_property =>
            Redirect(agent.controllers.routes.TermsController.showTerms())
          case IncomeSourceForm.option_both =>
            Redirect(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show())
        }
        case _ =>
          logging.info("Tried to submit 'other income error' when no data found in Keystore for 'income source'")
          throw new InternalServerException("Other Income Error controller, no income source found")
      }
  }

  lazy val backUrl: String = agent.controllers.routes.OtherIncomeController.showOtherIncome().url


}
