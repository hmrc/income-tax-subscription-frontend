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

import config.featureswitch.FeatureSwitch.TaxYear26To27Plus
import config.featureswitch.FeatureSwitching
import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.*
import play.api.mvc.*
import services.*
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
        sessionData <- sessionDataService.getAllSessionData()
        reference <- referenceRetrieval.getIndividualReference(sessionData)
        mandationStatus <- mandationStatusService.getMandationStatus(sessionData)
        eligibilityStatus <- getEligibilityStatusService.getEligibilityStatus(sessionData)
        selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
      } yield {
        Ok(whatYouNeedToDo(
          postAction = routes.WhatYouNeedToDoController.submit,
          backUrl = backUrl(
            eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly
          )
        ))
      }
  }

    val submit: Action[AnyContent] = Authenticated.async { implicit request =>
      _ =>
        for {
          sessionData <- sessionDataService.getAllSessionData()
          reference <- referenceRetrieval.getIndividualReference(sessionData)
          selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
        } yield {
          selectedTaxYear.map(_.accountingYear) match {
            case Some(Current) if isDisabled(TaxYear26To27Plus) => Redirect(controllers.individual.accountingperiod.routes.AccountingPeriodController.show)
            case _ => Redirect(controllers.individual.routes.UsingSoftwareController.show())
          }
        }
    }

  def backUrl(eligibleNextYearOnly: Boolean): String = {
    if (eligibleNextYearOnly) {
      controllers.individual.matching.routes.CannotUseServiceController.show().url
    } else {
      controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
    }
  }
}
