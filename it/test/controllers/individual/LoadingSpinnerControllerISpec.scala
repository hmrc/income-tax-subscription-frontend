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

package controllers.individual

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.ComponentSpecBase
import helpers.IntegrationTestModels.testAccountingYearCurrent
import helpers.servicemocks.AuthStub
import models.EligibilityStatus
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status.OK
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.SelectedTaxYear

class LoadingSpinnerControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.LoadingSpinnerController.show.url}" must {
    "return OK with the page content" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetAllSessionData(Map(
        ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
        ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
      ))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))

      When(s"GET ${routes.LoadingSpinnerController.show.url} is called")
      val result = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("loading-spinner.heading") + serviceNameGovUk)
      )
    }
  }
}
