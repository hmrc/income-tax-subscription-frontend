/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.AccountingYearForm
import models.AccountingYear
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import views.html.individual.incometax.business.WhatYearToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYearToSignUpController @Inject()(whatYearToSignUp: WhatYearToSignUp,
                                           val auditingService: AuditingService,
                                           val authService: AuthService,
                                           accountingPeriodService: AccountingPeriodService,
                                           val subscriptionDetailsService: SubscriptionDetailsService)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {

  def view(accountingYearForm: Form[AccountingYear], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    whatYearToSignUp(
      accountingYearForm = accountingYearForm,
      postAction = controllers.individual.business.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        subscriptionDetailsService.fetchSelectedTaxYear(reference) map { accountingYearModel =>
          Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYearModel.map(aym => aym.accountingYear)), isEditMode = isEditMode))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        AccountingYearForm.accountingYearForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
          accountingYear => {
            subscriptionDetailsService.saveSelectedTaxYear(reference, AccountingYearModel(accountingYear)) map { _ =>
              Redirect(controllers.individual.business.routes.TaxYearCheckYourAnswersController.show())
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): Option[String] = {
    if (isEditMode) {
      Some(controllers.individual.business.routes.TaxYearCheckYourAnswersController.show().url)
    } else {
      Some(controllers.individual.business.routes.TaskListController.show.url)
    }
  }
}
