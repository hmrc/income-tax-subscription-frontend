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

package controllers.agent

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{AgentURI, basGatewaySignIn, testNino}
import helpers.IntegrationTestModels.testAccountingYearCurrent
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import models.{EligibilityStatus, Yes, YesNo}
import org.mockito.Mockito._
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.SelectedTaxYear
import utilities.agent.TestConstants.testUtr

class ClientLoadingSpinnerControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.ClientLoadingSpinnerController.show.url}" must {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/confirming-please-wait"))
        )
      }
    }
    "return OK with the page content" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetAllSessionData(Map(
        ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
        ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason= None)),
        ITSASessionKeys.NINO -> JsString(testNino),
        ITSASessionKeys.UTR -> JsString(testUtr)
      ))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))

      When(s"GET ${routes.ClientLoadingSpinnerController.show.url} is called")
      val result = IncomeTaxSubscriptionFrontend.loadingConfirmationStatus()

      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("loading-spinner.heading") + serviceNameGovUk)
      )
    }
  }

}