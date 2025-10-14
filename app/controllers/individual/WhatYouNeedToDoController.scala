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

package controllers.individual

import _root_.config.featureswitch.FeatureSwitch.EmailCaptureConsent
import _root_.config.featureswitch.FeatureSwitching
import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models._
import models.status.MandationStatus.Mandated
import play.api.mvc._
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.WhatYouNeedToDo

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToDoController @Inject()(whatYouNeedToDo: WhatYouNeedToDo,
                                          mandationStatusService: MandationStatusService,
                                          getEligibilityStatusService: GetEligibilityStatusService,
                                          referenceRetrieval: ReferenceRetrieval,
                                          subscriptionDetailsService: SubscriptionDetailsService,
                                          sessionDataService: SessionDataService)
                                         (val auditingService: AuditingService,
                                          val appConfig: AppConfig,
                                          val authService: AuthService)
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends SignUpController with FeatureSwitching {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        reference <- referenceRetrieval.getIndividualReference
        mandationStatus <- mandationStatusService.getMandationStatus
        eligibilityStatus <- getEligibilityStatusService.getEligibilityStatus
        usingSoftwareStatus <- sessionDataService.fetchSoftwareStatus
        selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
        consentStatus <- sessionDataService.fetchConsentStatus
      } yield {
        val taxYearSelection: Option[AccountingYear] = selectedTaxYear.map(_.accountingYear)
        val consentYesNo: Option[YesNo] = consentStatus match {
          case Left(_) => throw new InternalServerException("[WhatYouNeedToDoController][show] - Could not fetch email consent status")
          case Right(yesNo) => yesNo
        }
        usingSoftwareStatus match {
          case Left(_) => throw new InternalServerException("[WhatYouNeedToDoController][show] - Could not fetch software status")
          case Right(selectedSoftwareStatus) =>
            Ok(whatYouNeedToDo(
              postAction = routes.WhatYouNeedToDoController.submit,
              onlyNextYear = eligibilityStatus.eligibleNextYearOnly,
              mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
              mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
              isUsingSoftware = selectedSoftwareStatus.contains(Yes),
              signUpNextTaxYear = taxYearSelection.contains(Next),
              backUrl = backUrl(
                eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly,
                mandatedCurrentYear = mandationStatus.currentYearStatus == Mandated,
                consentStatus = consentYesNo,
                taxYearSelection = taxYearSelection
              )
            ))
        }
      }
  }

  val submit: Action[AnyContent] = Authenticated { _ =>
    _ => Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
  }

  def backUrl(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, consentStatus: Option[YesNo], taxYearSelection: Option[AccountingYear]): String = {

    if (isEnabled(EmailCaptureConsent)) {
      if(eligibleNextYearOnly) {
        controllers.individual.routes.UsingSoftwareController.show(false).url
      } else {
        (taxYearSelection, consentStatus) match {
          case (Some(Current), Some(Yes)) => controllers.individual.email.routes.EmailCaptureController.show().url
          case (Some(Current), Some(No)) => controllers.individual.email.routes.CaptureConsentController.show().url
          case _ => controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        }
      }
    } else {
      if (eligibleNextYearOnly || mandatedCurrentYear) {
        controllers.individual.routes.UsingSoftwareController.show(false).url
      } else if (taxYearSelection.contains(Current)){
        controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url
      } else {
        controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
      }
    }
  }

}
