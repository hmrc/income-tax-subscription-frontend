/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.individual.tasklist.taxyear

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.AccountingYearForm
import models.AccountingYear
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.*
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.taxyear.WhenDoYouWantToStart
import config.featureswitch.FeatureSwitch.TaxYear26To27Plus
import config.featureswitch.FeatureSwitching

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhenDoYouWantToStartController @Inject()(whenDoYouWantToStart: WhenDoYouWantToStart,
                                               accountingPeriodService: AccountingPeriodService,
                                               referenceRetrieval: ReferenceRetrieval,
                                               subscriptionDetailsService: SubscriptionDetailsService,
                                               sessionDataService: SessionDataService)
                                              (val auditingService: AuditingService,
                                               val authService: AuthService,
                                               val appConfig: AppConfig)
                                              (implicit val ec: ExecutionContext,
                                               mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def view(accountingYearForm: Form[AccountingYear], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    whenDoYouWantToStart(
      accountingYearForm = accountingYearForm,
      postAction = controllers.individual.tasklist.taxyear.routes.WhenDoYouWantToStartController.submit(editMode = isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode,
      hideContent = isEnabled(TaxYear26To27Plus)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      sessionDataService.getAllSessionData().flatMap { sessionData =>
        referenceRetrieval.getIndividualReference(sessionData) flatMap { reference =>
          subscriptionDetailsService.fetchSelectedTaxYear(reference) map {
            case Some(taxYearModel) if !taxYearModel.editable =>
              Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
            case accountingYearModel =>
              Ok(view(
                accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYearModel.map(_.accountingYear)),
                isEditMode = isEditMode
              ))
          }
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      sessionDataService.getAllSessionData().flatMap { sessionData =>
        referenceRetrieval.getIndividualReference(sessionData) flatMap { reference =>
          AccountingYearForm.accountingYearForm.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
            accountingYear => {
              subscriptionDetailsService.saveSelectedTaxYear(reference, AccountingYearModel(accountingYear)) map {
                case Right(_) =>
                  if (isEditMode) {
                    Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
                  } else {
                    Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
                  }
                case Left(_) =>
                  throw new InternalServerException("[WhenDoYouWantToStartController][submit] - Could not save accounting year")
              }
            }
          )
        }
      }
  }
}
