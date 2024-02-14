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

package controllers.agent.matching

import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.ReturnToClientDetailsModel.{ContinueWithCurrentClient, SignUpAnotherClient}
import play.api.http.Status._


class ReturnToClientDetailsControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/return-to-client-details" should {
    "show the Return To Client Details page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /client/business/return-to-client-details is called")
      val res = IncomeTaxSubscriptionFrontend.showReturnToClientDetails()


      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      Then("Should return a OK with the Cannot Go Back To Previous Client page")
      res must have(
        httpStatus(200),
        pageTitle(messages("agent.return-to-client-details.title") + serviceNameGovUk),
        radioButtonSet(id = "returnToClientDetails", selectedRadioButton = None),
        radioButtonSet(id = "returnToClientDetails-2", selectedRadioButton = None)
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/return-to-client-details" should {
    "redirect to Current Client Eligibility Questions page" when {
      "selecting the Continue with current client radio button" in {
        val userInput = ContinueWithCurrentClient

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/return-to-client-details is called")
        val res = IncomeTaxSubscriptionFrontend.submitReturnToClientDetails(Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Agent Service Account")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.AccountingPeriodCheckController.show.url)
        )
      }
      "selecting the Sign Up Another Client radio button" in {
        val userInput = SignUpAnotherClient

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/cannot-go-back-to-previous-client is called")
        val res = IncomeTaxSubscriptionFrontend.submitReturnToClientDetails(Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Enter Client Details")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.AddAnotherClientController.addAnother().url)
        )
      }
    }
    "return an error page(BAD Request)" when {
      "no input is selected" in {

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the Cannot Go Back To Previous Client page")

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/return-to-client-details is called")
        val res = IncomeTaxSubscriptionFrontend.submitReturnToClientDetails(None)

        Then("Should return a SEE_OTHER with a redirect location of Enter Client Details")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.return-to-client-details.title") + serviceNameGovUk),
          errorDisplayed()
        )
      }
    }
  }
}


