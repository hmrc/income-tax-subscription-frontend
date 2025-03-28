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

package controllers.individual.tasklist.taxyear

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import controllers.utils.ReferenceRetrieval
import forms.individual.business.AccountingYearForm
import models.common.AccountingYearModel
import models.{AccountingYear, Current}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.taxyear.WhatYearToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYearToSignUpController @Inject()(whatYearToSignUp: WhatYearToSignUp,
                                           accountingPeriodService: AccountingPeriodService,
                                           referenceRetrieval: ReferenceRetrieval,
                                           subscriptionDetailsService: SubscriptionDetailsService)
                                          (val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val appConfig: AppConfig)
                                          (implicit val ec: ExecutionContext,
                                           mcc: MessagesControllerComponents) extends SignUpController {

  def view(accountingYearForm: Form[AccountingYear], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    whatYearToSignUp(
      accountingYearForm = accountingYearForm,
      postAction = controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
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

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        AccountingYearForm.accountingYearForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
          accountingYear => {
            subscriptionDetailsService.saveSelectedTaxYear(reference, AccountingYearModel(accountingYear)) map {
              case Right(_) =>
                if (isEditMode) {
                  Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
                } else {
                  accountingYear match {
                    case Current if isEnabled(EmailCaptureConsent) =>
                      Redirect(controllers.individual.email.routes.CaptureConsentController.show())
                    case _ =>
                      Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
                  }
                }
              case Left(_) =>
                throw new InternalServerException("[WhatYearToSignUpController][submit] - Could not save accounting year")
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): Option[String] = {
    if (isEditMode) {
      Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)
    } else {
      Some(controllers.individual.routes.UsingSoftwareController.show().url)
    }
  }
}
