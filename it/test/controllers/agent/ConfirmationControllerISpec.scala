/*
 * Copyright 2018 HM Revenue & Customs
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
import helpers.IntegrationTestConstants.testNino
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.EligibilityStatus
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys._

class ConfirmationControllerISpec extends ComponentSpecBase {

  "GET /confirmation" when {
    s"There is ${ITSASessionKeys.MTDITID} in session" should {
      "call subscription on the back end service" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(
          responseStatus = OK,
          responseBody = Json.toJson(MandationStatusModel(Voluntary, Voluntary))
        )
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(
          responseStatus = OK,
          responseBody = Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        )
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

        When("I call GET /confirmation")
        val res = IncomeTaxSubscriptionFrontend.showConfirmation(hasSubmitted = true, "Test", "User", "A111111AA")

        Then("The result must have a status of OK and display the confirmation page")
        res must have(
          httpStatus(OK)
        )
      }
    }

    s"There is not ${ITSASessionKeys.MTDITID} in session" should {
      "call subscription on the back end service" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("I call GET /confirmation")
        val res = IncomeTaxSubscriptionFrontend.showConfirmation(hasSubmitted = false, "Test", "User", "A111111AA")

        Then("The result must have a status of NOT_FOUND")
        res must have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }
}
