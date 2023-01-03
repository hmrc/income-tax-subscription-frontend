/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.agent.AccountingYearForm
import models.AccountingYear
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.business.WhatYearToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYearToSignUpController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           accountingPeriodService: AccountingPeriodService,
                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                           whatYearToSignUp: WhatYearToSignUp)
                                          (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                           val appConfig: AppConfig) extends AuthenticatedController with ReferenceRetrieval  {

  def backUrl(isEditMode: Boolean): Option[String] =
    if(isEditMode)
      Some(controllers.agent.routes.TaxYearCheckYourAnswersController.show().url)
    else
      Some(controllers.agent.routes.TaskListController.show().url)

  def view(accountingYearForm: Form[AccountingYear], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    whatYearToSignUp(
      accountingYearForm = accountingYearForm,
      postAction = controllers.agent.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchSelectedTaxYear(reference) map { accountingYearModel =>
          Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYearModel.map(aym => aym.accountingYear)),
            isEditMode = isEditMode))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        AccountingYearForm.accountingYearForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
          accountingYear => {
            subscriptionDetailsService.saveSelectedTaxYear(reference, AccountingYearModel(accountingYear)) map {
              case Right(_) => Redirect(controllers.agent.routes.TaxYearCheckYourAnswersController.show())
              case Left(_) => throw new InternalServerException("[WhatYearToSignUpController][submit] - Could not save accounting year")
            }
          }
        )
      }
  }
}
