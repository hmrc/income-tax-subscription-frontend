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
import agent.forms.AccountingMethodForm
import agent.models.AccountingMethodModel
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.{AgentPropertyCashOrAccruals, AgentTaxYear, EligibilityPagesFeature, FeatureSwitching}
import core.models.{No, Yes}
import core.services.AuthService
import incometax.subscription.models.Both
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class BusinessAccountingMethodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService,
                                                   val authService: AuthService
                                                  ) extends AuthenticatedController with FeatureSwitching {

  def view(accountingMethodForm: Form[AccountingMethodModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] =
    backUrl(isEditMode).map { backUrl =>
      agent.views.html.business.accounting_method(
        accountingMethodForm = accountingMethodForm,
        postAction = agent.controllers.business.routes.BusinessAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = backUrl
      )
    }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchAccountingMethod() flatMap { accountingMethod =>
        view(accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(accountingMethod), isEditMode = isEditMode).map(html => Ok(html))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
        formWithErrors => view(accountingMethodForm = formWithErrors, isEditMode = isEditMode) map(html=> BadRequest(html)),
        accountingMethod => {
          for {
            _ <- keystoreService.saveAccountingMethod(accountingMethod)
            incomeSource <- keystoreService.fetchIncomeSource()
          } yield {
            if (isEditMode) {
              Redirect(agent.controllers.routes.CheckYourAnswersController.show())
            } else if (isEnabled(AgentPropertyCashOrAccruals) && incomeSource.contains(Both)) {
              Redirect(agent.controllers.business.routes.PropertyAccountingMethodController.show())
            } else if (isEnabled(EligibilityPagesFeature)) {
              Redirect(agent.controllers.routes.CheckYourAnswersController.show())
            } else {
              Redirect(agent.controllers.routes.TermsController.show())
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit request: Request[_]): Future[String] =
    if (isEditMode)
      Future.successful(agent.controllers.routes.CheckYourAnswersController.show().url)
    else {
      if (isEnabled(AgentTaxYear)) {
        keystoreService.fetchMatchTaxYear() map {
          case Some(matchTaxYear) => matchTaxYear.matchTaxYear match {
            case Yes =>
              agent.controllers.business.routes.MatchTaxYearController.show().url
            case No =>
              agent.controllers.business.routes.BusinessAccountingPeriodDateController.show().url
          }
          case _ => throw new InternalServerException(s"Internal Server Error - No Match Tax Year answer retrieved from keystore")
        }
      }
      else
        Future.successful(agent.controllers.business.routes.BusinessAccountingPeriodDateController.show().url)
    }
}
