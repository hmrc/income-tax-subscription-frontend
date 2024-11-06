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

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import controllers.utils.ReferenceRetrieval
import models.{Next, Yes}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.InternalServerException
import utilities.AccountingPeriodUtil
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
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends SignUpController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        reference <- referenceRetrieval.getIndividualReference
        mandationStatus <- mandationStatusService.getMandationStatus
        eligibilityStatus <- getEligibilityStatusService.getEligibilityStatus
        usingSoftwareStatus <- sessionDataService.fetchSoftwareStatus
        selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
      } yield {
        val selectedNextTaxYear: Boolean = selectedTaxYear.map(_.accountingYear).contains(Next)
        usingSoftwareStatus match {
          case Left(_) => throw new InternalServerException("[UsingSoftwareController][show] - Could not fetch software status")
          case Right(selectedSoftwareStatus) =>
            Ok(whatYouNeedToDo(
              postAction = routes.WhatYouNeedToDoController.submit,
              onlyNextYear = eligibilityStatus.eligibleNextYearOnly,
              mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
              mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
              isUsingSoftware = selectedSoftwareStatus.contains(Yes),
              signUpNextTaxYear = selectedNextTaxYear,
              backUrl = backUrl
            ))
        }
      }
  }

  val submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      if (isEnabled(PrePopulate)) {
        Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
      } else {
        Redirect(controllers.individual.tasklist.routes.TaskListController.show())
      }
  }

  def backUrl: String = {
    if (isEnabled(PrePopulate)) {
      controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
    } else {
      controllers.individual.routes.UsingSoftwareController.show().url
    }
  }
}
