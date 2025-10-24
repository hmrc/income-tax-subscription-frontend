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
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino}
import helpers.agent.ComponentSpecBase
import helpers.agent.ComponentSpecBase.reference
import helpers.agent.servicemocks.AuthStub
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import models.{EligibilityStatus, Yes, YesNo}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys._
import utilities.agent.TestConstants.testUtr

class ConfirmationControllerISpec extends ComponentSpecBase {

  "GET /confirmation" when {
    "the user is unauthenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.showConfirmation(hasSubmitted = true, "Test", "User", "AA111111A")

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/confirmation"))
        )
      }
    }

    s"There is ${ITSASessionKeys.MTDITID} in session" should {
      "call subscription on the back end service" in {
        val testOption: YesNo = Yes
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)),
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.HAS_SOFTWARE -> Json.toJson(testOption),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

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

  "POST /confirmation" should {
    "redirect to the login page" should {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.submitConfirmation()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/confirmation"))
        )
      }
    }
    "redirect to the add another client route" when {
      "deleting all saved subscription details data was successful" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        IncomeTaxSubscriptionConnectorStub.stubDeleteAllSubscriptionDetails(reference)(OK)

        val res = IncomeTaxSubscriptionFrontend.submitConfirmation()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddAnotherClientController.addAnother().url)
        )
      }
    }
    "return an internal server error" when {
      "there was a problem deleting data from the subscription details" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        IncomeTaxSubscriptionConnectorStub.stubDeleteAllSubscriptionDetails(reference)(INTERNAL_SERVER_ERROR)

        val res = IncomeTaxSubscriptionFrontend.submitConfirmation()

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

}
