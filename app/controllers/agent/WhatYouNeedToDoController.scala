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

import config.AppConfig
import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models._
import models.status.MandationStatus.Mandated
import play.api.mvc._
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.WhatYouNeedToDo

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToDoController @Inject()(view: WhatYouNeedToDo,
                                          identify: IdentifierAction,
                                          journeyRefiner: ConfirmedClientJourneyRefiner,
                                          eligibilityStatusService: GetEligibilityStatusService,
                                          mandationStatusService: MandationStatusService,
                                          subscriptionDetailsService: SubscriptionDetailsService,
                                          sessionDataService: SessionDataService
                                         )(val appConfig: AppConfig)
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext)
  extends SignUpBaseController with FeatureSwitching {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      eligibilityStatus <- eligibilityStatusService.getEligibilityStatus
      mandationStatus <- mandationStatusService.getMandationStatus
      taxYearSelection <- subscriptionDetailsService.fetchSelectedTaxYear(request.reference)
      softwareStatus <- sessionDataService.fetchSoftwareStatus
      captureConsentStatus <- sessionDataService.fetchConsentStatus
    } yield {
      val isCurrentYear = taxYearSelection.map(_.accountingYear).contains(Current)
      val consentYesNo: Option[YesNo] = captureConsentStatus match {
        case Left(_) => throw new InternalServerException("[WhatYouNeedToDoController][show] - Could not fetch email consent status")
        case Right(yesNo) => yesNo
      }
      val usingSoftwareStatus: Boolean = softwareStatus match {
        case Right(Some(Yes)) => true
        case Right(Some(No)) => false
        case Right(None) => false
        case Left(error) =>
          logger.error(s"[ConfirmationController][show] - failure retrieving software status - $error")
          false
      }
      Ok(view(
        postAction = routes.WhatYouNeedToDoController.submit,
        eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly,
        mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
        mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
        taxYearSelectionIsCurrent = isCurrentYear,
        usingSoftwareStatus = usingSoftwareStatus,
        clientName = request.clientDetails.name,
        clientNino = request.clientDetails.formattedNino,
        backUrl = Some(backUrl(
          eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly,
          mandatedCurrentYear = mandationStatus.currentYearStatus == Mandated,
          captureConsentStatus = consentYesNo,
          taxYearSelection = taxYearSelection.map(_.accountingYear)
        ))
      ))
    }
  }

  val submit: Action[AnyContent] = (identify andThen journeyRefiner) { _ =>
    Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
  }

  def backUrl(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, captureConsentStatus: Option[YesNo], taxYearSelection: Option[AccountingYear]): String = {
    if (isEnabled(EmailCaptureConsent)) {
      if (eligibleNextYearOnly) {
        controllers.agent.routes.UsingSoftwareController.show.url
      } else {
        (taxYearSelection, captureConsentStatus) match {
          case (Some(Current), Some(Yes)) => controllers.agent.email.routes.EmailCaptureController.show().url
          case (Some(Current), Some(No)) => controllers.agent.email.routes.CaptureConsentController.show().url
          case _ => controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        }
      }
    } else {
      if (!(eligibleNextYearOnly || mandatedCurrentYear)) {
        controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
      } else {
        controllers.agent.routes.UsingSoftwareController.show.url
      }
    }
  }
}