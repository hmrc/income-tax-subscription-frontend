/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.individual

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.UsingSoftwareForm
import models.{AccountingYear, Current, No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.*
import play.twirl.api.Html
import services.*
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.UsingSoftware

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class UsingSoftwareController @Inject()(usingSoftware: UsingSoftware,
                                        sessionDataService: SessionDataService,
                                        eligibilityStatusService: GetEligibilityStatusService,
                                        mandationStatusService: MandationStatusService,
                                        referenceRetrieval: ReferenceRetrieval,
                                        subscriptionDetailsService: SubscriptionDetailsService
                                       )
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents)
  extends SignUpController {

  private val form: Form[YesNo] = UsingSoftwareForm.usingSoftwareForm


  def view(usingSoftwareForm: Form[YesNo], editMode: Boolean, backUrl: String)
          (implicit request: Request[_]): Html = {
    usingSoftware(
      usingSoftwareForm = usingSoftwareForm,
      postAction = controllers.individual.routes.UsingSoftwareController.submit(editMode),
      backUrl = backUrl
    )
  }

  def show(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        sessionData <- sessionDataService.getAllSessionData()
        eligibilityStatus <- eligibilityStatusService.getEligibilityStatus(sessionData)
        reference <- referenceRetrieval.getIndividualReference(sessionData)
        selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
      } yield {
        val usingSoftwareStatus = sessionData.fetchSoftwareStatus
        val consentStatus = sessionData.fetchConsentStatus
        val taxYearSelection = selectedTaxYear.map(_.accountingYear)
        Ok(view(
          usingSoftwareForm = form.fill(usingSoftwareStatus),
          editMode = editMode,
          backUrl = backUrl(
            editMode = editMode,
            eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly,
            taxYearSelection = taxYearSelection,
            consentStatus = consentStatus
          )
        ))
      }
  }

  def submit(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      form.bindFromRequest().fold(
        formWithErrors =>
          for {
            sessionData <- sessionDataService.getAllSessionData()
            eligibilityStatus <- eligibilityStatusService.getEligibilityStatus(sessionData)
            reference <- referenceRetrieval.getIndividualReference(sessionData)
            selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
          } yield {
            val consentStatus = sessionData.fetchConsentStatus
            val taxYearSelection = selectedTaxYear.map(_.accountingYear)
            BadRequest(view(
              usingSoftwareForm = formWithErrors,
              editMode = editMode,
              backUrl = backUrl(
                editMode = editMode,
                eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly,
                taxYearSelection = taxYearSelection,
                consentStatus = consentStatus
              )
            ))
          },
        yesNo =>
          for {
            sessionData <- sessionDataService.getAllSessionData()
            usingSoftwareStatus <- sessionDataService.saveSoftwareStatus(yesNo)
            eligibilityStatus <- eligibilityStatusService.getEligibilityStatus(sessionData)
            mandationStatus <- mandationStatusService.getMandationStatus(sessionData)
          } yield {
            val isMandatedCurrentYear: Boolean = mandationStatus.currentYearStatus.isMandated
            val isEligibleNextYearOnly: Boolean = eligibilityStatus.eligibleNextYearOnly

            usingSoftwareStatus match {
              case Left(_) =>
                throw new InternalServerException("[UsingSoftwareController][submit] - Could not save using software answer")
              case Right(_) =>
                if (editMode) {
                  Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
                } else if (isMandatedCurrentYear) {
                  Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
                } else {
                  Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
                }
            }
          }
      )
  }

  private def backUrl(editMode: Boolean, eligibleNextYearOnly: Boolean, taxYearSelection: Option[AccountingYear], consentStatus: Option[YesNo]): String = {
    if (editMode) {
      controllers.individual.routes.GlobalCheckYourAnswersController.show.url
    } else {
      if (eligibleNextYearOnly) {
        controllers.individual.routes.WhatYouNeedToDoController.show.url
      } else {
        (taxYearSelection, consentStatus) match {
          case (Some(Current), Some(Yes)) => controllers.individual.email.routes.EmailCaptureController.show().url
          case (Some(Current), Some(No)) => controllers.individual.email.routes.CaptureConsentController.show().url
          case (Some(Current), None) => controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url
          case _ => controllers.individual.routes.WhatYouNeedToDoController.show.url
        }
      }
    }
  }
}
