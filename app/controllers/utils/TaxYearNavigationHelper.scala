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

package controllers.utils

import auth.agent.IncomeTaxAgentUser
import auth.individual.IncomeTaxSAUser
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import services.{GetEligibilityStatusService, MandationStatusService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait TaxYearNavigationHelper {

  val mandationStatusService: MandationStatusService
  val getEligibilityStatusService: GetEligibilityStatusService

  def handleUnableToSelectTaxYearIndividual(ableToSelect: Future[Result])
                                           (implicit request: Request[AnyContent],
                                            user: IncomeTaxSAUser,
                                            hc: HeaderCarrier,
                                            ec: ExecutionContext): Future[Result] = {

    mandationStatusService.getMandationStatus(user.getUtr) flatMap { mandationStatus =>
      getEligibilityStatusService.getEligibilityStatus(user.getUtr) flatMap { eligibilityStatus =>
        val isMandatedCurrentYear: Boolean = mandationStatus.currentYearStatus.isMandated
        val isEligibleNextYearOnly: Boolean = eligibilityStatus.eligibleNextYearOnly

        if (isMandatedCurrentYear || isEligibleNextYearOnly) {
          Future.successful(Redirect(controllers.individual.tasklist.routes.TaskListController.show()))
        } else {
          ableToSelect
        }
      }
    }

  }

  def handleUnableToSelectTaxYearAgent(ableToSelect: Future[Result])
                                      (implicit request: Request[AnyContent],
                                       user: IncomeTaxAgentUser,
                                       hc: HeaderCarrier,
                                       ec: ExecutionContext): Future[Result] = {

    mandationStatusService.getMandationStatus(user.getClientUtr) flatMap { mandationStatus =>
      getEligibilityStatusService.getEligibilityStatus(user.getClientUtr) flatMap { eligibilityStatus =>
        val isMandatedCurrentYear: Boolean = mandationStatus.currentYearStatus.isMandated
        val isEligibleNextYearOnly: Boolean = eligibilityStatus.eligibleNextYearOnly

        if (isMandatedCurrentYear || isEligibleNextYearOnly) {
          Future.successful(Redirect(controllers.agent.tasklist.routes.TaskListController.show()))
        } else {
          ableToSelect
        }
      }
    }
  }

}
