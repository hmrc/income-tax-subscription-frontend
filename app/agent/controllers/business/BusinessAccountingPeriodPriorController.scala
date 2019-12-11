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

package agent.controllers.business

import agent.auth.AuthenticatedController
import agent.forms.AccountingPeriodPriorForm
import agent.models.AccountingPeriodPriorModel
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.{AgentTaxYear, EligibilityPagesFeature, FeatureSwitching}
import core.models.{No, Yes}
import core.services.AuthService
import core.utils.Implicits._
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class BusinessAccountingPeriodPriorController @Inject()(val baseConfig: BaseControllerConfig,
                                                        val messagesApi: MessagesApi,
                                                        val keystoreService: KeystoreService,
                                                        val authService: AuthService
                                                       ) extends AuthenticatedController with FeatureSwitching {

  def view(accountingPeriodPriorForm: Form[AccountingPeriodPriorModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] =
    backUrl.map { backUrl =>
      agent.views.html.business.accounting_period_prior(
        accountingPeriodPriorForm = accountingPeriodPriorForm,
        postAction = agent.controllers.business.routes.BusinessAccountingPeriodPriorController.submit(editMode = isEditMode),
        backUrl = backUrl,
        isEditMode = isEditMode
      )
    }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchAccountingPeriodPrior().flatMap { x =>
        view(AccountingPeriodPriorForm.accountingPeriodPriorForm.fill(x), isEditMode = isEditMode).map(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingPeriodPriorForm.accountingPeriodPriorForm.bindFromRequest.fold(
        formWithErrors => view(formWithErrors, isEditMode = isEditMode).flatMap(view => BadRequest(view)),
        accountingPeriodPrior =>
          keystoreService.fetchAccountingPeriodPrior().flatMap {
            somePreviousAnswer =>
              keystoreService.saveAccountingPeriodPrior(accountingPeriodPrior) flatMap { _ =>
                if (somePreviousAnswer.fold(false)(previousAnswer => previousAnswer.equals(accountingPeriodPrior)) && isEditMode)
                  Redirect(agent.controllers.routes.CheckYourAnswersController.show())
                else {
                  accountingPeriodPrior.currentPeriodIsPrior match {
                    case Yes => yes
                    case No => no
                  }
                }
              }
          }
      )
  }

  def yes(implicit request: Request[_]): Future[Result] = Redirect(agent.controllers.business.routes.RegisterNextAccountingPeriodController.show())

  def no(implicit request: Request[_]): Future[Result] = {
    if (isEnabled(AgentTaxYear)) Redirect(agent.controllers.business.routes.MatchTaxYearController.show())
    else Redirect(agent.controllers.business.routes.BusinessAccountingPeriodDateController.show())
  }

  def backUrl(implicit request: Request[_]): Future[String] = {
    if (isEnabled(EligibilityPagesFeature)) {
      Future.successful(agent.controllers.business.routes.BusinessNameController.show().url)
    } else {
      keystoreService.fetchOtherIncome().map {
        case Some(Yes) => agent.controllers.routes.OtherIncomeErrorController.show().url
        case _ => agent.controllers.routes.OtherIncomeController.show().url
      }
    }
  }

}
