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
import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import controllers.utils.ReferenceRetrieval
import models.{Current, No, Yes}
import play.api.mvc._
import services._
import services.agent.ClientDetailsRetrieval
import views.html.agent.WhatYouNeedToDo

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class WhatYouNeedToDoController @Inject()(whatYouNeedToDo: WhatYouNeedToDo,
                                          clientDetailsRetrieval: ClientDetailsRetrieval,
                                          eligibilityStatusService: GetEligibilityStatusService,
                                          mandationStatusService: MandationStatusService,
                                          referenceRetrieval: ReferenceRetrieval,
                                          subscriptionDetailsService: SubscriptionDetailsService,
                                          sessionDataService: SessionDataService)
                                         (val auditingService: AuditingService,
                                          val appConfig: AppConfig,
                                          val authService: AuthService)
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends AuthenticatedController with FeatureSwitching {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }

  def backUrl(eligibleNextYearOnly: Boolean): Option[String] = {
    if (isEnabled(PrePopulate)) {
      if (eligibleNextYearOnly)
        Some(controllers.agent.routes.UsingSoftwareController.show.url)
      else
        Some(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
    } else None
  }


  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        clientDetails <- clientDetailsRetrieval.getClientDetails
        eligibilityStatus <- eligibilityStatusService.getEligibilityStatus
        mandationStatus <- mandationStatusService.getMandationStatus
        reference <- referenceRetrieval.getAgentReference
        taxYearSelection <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
        softwareStatus <- sessionDataService.fetchSoftwareStatus
      } yield {
        val isCurrentYear = taxYearSelection.map(_.accountingYear).contains(Current)

        val usingSoftwareStatus: Boolean = softwareStatus match {
          case Right(Some(Yes)) => true
          case Right(Some(No)) => false
          case Right(None) => false
          case Left(error) =>
            logger.error(s"[ConfirmationController][show] - failure retrieving software status - $error")
            false
        }
        Ok(whatYouNeedToDo(
          postAction = routes.WhatYouNeedToDoController.submit,
          eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly,
          mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
          mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
          taxYearSelectionIsCurrent = isCurrentYear,
          usingSoftwareStatus = usingSoftwareStatus,
          clientName = clientDetails.name,
          clientNino = formatNino(clientDetails.nino),
          backUrl = backUrl(eligibilityStatus.eligibleNextYearOnly)
        ))
      }
  }

  val submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      if (isEnabled(PrePopulate)) Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
      else Redirect(controllers.agent.tasklist.routes.TaskListController.show())
  }

}
