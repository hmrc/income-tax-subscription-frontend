/*
 * Copyright 2019 HM Revenue & Customs
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

import agent.auth.AuthenticatedController
import agent.forms.IncomeSourceForm
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.{AgentPropertyCashOrAccruals, FeatureSwitching}
import core.models.{No, Yes}
import core.services.AuthService
import core.utils.Implicits._
import incometax.subscription.models.{Both, Business, Property}
import incometax.util.AccountingPeriodUtil
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class TermsController @Inject()(val baseConfig: BaseControllerConfig,
                                val messagesApi: MessagesApi,
                                val keystoreService: KeystoreService,
                                val authService: AuthService
                               ) extends AuthenticatedController with FeatureSwitching {

  def view(backUrl: String, taxEndYear: Int)(implicit request: Request[_]): Html =
    agent.views.html.terms(
      postAction = agent.controllers.routes.TermsController.submit(),
      taxEndYear = taxEndYear,
      backUrl
    )

  def show(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        incomeSource <- keystoreService.fetchIncomeSource().collect { case Some(is) => is.source }
        taxEndYear <- incomeSource match {
          case IncomeSourceForm.option_property => Future.successful(AccountingPeriodUtil.getCurrentTaxEndYear)
          case _ => keystoreService.fetchAccountingPeriodDate().collect { case Some(ad) => AccountingPeriodUtil.getTaxEndYear(ad) }
        }
        backUrl <- backUrl(editMode)
      } yield Ok(view(backUrl = backUrl, taxEndYear = taxEndYear))
  }

  def submit(isEditMode: Boolean = false): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.saveTerms(terms = true) map (
        _ => Redirect(agent.controllers.routes.CheckYourAnswersController.show()))
  }

  def backUrl(editMode: Boolean)(implicit request: Request[_]): Future[String] = {
    if (editMode) {
      agent.controllers.business.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
    } else {
      keystoreService.fetchIncomeSource() flatMap {
        case Some(Business) => agent.controllers.business.routes.BusinessAccountingMethodController.show().url
        case Some(Both | Property) if isEnabled(AgentPropertyCashOrAccruals) => agent.controllers.business.routes.PropertyAccountingMethodController.show().url
        case Some(Both) => agent.controllers.business.routes.BusinessAccountingMethodController.show().url
        case Some(Property) => agent.controllers.routes.IncomeSourceController.show().url
        case _ => throw new InternalServerException(s"Internal Server Error - TermsController.backUrl, no income source retrieve from Keystore")
      }
    }
  }

}