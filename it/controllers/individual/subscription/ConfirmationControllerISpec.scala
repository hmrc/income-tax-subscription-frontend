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

package controllers.individual.subscription

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{signOutURI, testSubscriptionId}
import helpers.IntegrationTestModels.testIncomeSourceIndivProperty
import helpers.servicemocks.AuthStub
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys._

class ConfirmationControllerISpec extends ComponentSpecBase {

  "GET /confirmation" should {
    "return the confirmation page when the user is enrolled and confirm SPS preferences" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(Map(
        IncomeSource -> Json.toJson(testIncomeSourceIndivProperty),
        MtditId -> JsString(testSubscriptionId)
      ))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

      When("GET /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.confirmation()
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      Then("Should return a OK with the confirmation page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("sign-up-complete.heading") + serviceNameGovUk)
      )
    }

    "return a NOT_FOUND when the user is not enrolled" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.confirmation()

      Then("Should return a NOT_FOUND status")
      res must have(
        httpStatus(NOT_FOUND))
    }
  }

  "POST /confirmation" should {
    "redirect the user to sign out" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()

      When("POST /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.submitConfirmation()

      Then("Should redirect to sign out")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(signOutURI)
      )
    }
    "return a NOT_FOUND when the user is not enrolled" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.submitConfirmation()

      Then("Should return a NOT_FOUND status")
      res must have(
        httpStatus(NOT_FOUND)
      )

    }
  }

}
