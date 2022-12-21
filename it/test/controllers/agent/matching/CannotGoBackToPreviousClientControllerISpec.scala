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
import models.CannotGoBack.{AgentServiceAccount, ReenterClientDetails, SignUpAnotherClient}
import play.api.http.Status._


class CannotGoBackToPreviousClientControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/cannot-go-back-to-previous-client" should {
    "show the Cannot Go Back To Previous Client page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /client/business/what-year-to-sign-up is called")
      val res = IncomeTaxSubscriptionFrontend.showCannotGoBackToPreviousClient()


      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      Then("Should return a OK with the Cannot Go Back To Previous Client page")
      res must have(
        httpStatus(200),
        pageTitle(messages("agent.cannot-go-back-previous-client.title") + serviceNameGovUk),
        radioButtonSet(id = "cannotGoBackToPreviousClient", selectedRadioButton = None),
        radioButtonSet(id = "cannotGoBackToPreviousClient-2", selectedRadioButton = None),
        radioButtonSet(id = "cannotGoBackToPreviousClient-3", selectedRadioButton = None)
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/cannot-go-back-to-previous-client" should {
    "redirect to Agent Service Account" when {
      "selecting the Agent Service Account radio button" in {
        val userInput = AgentServiceAccount

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/cannot-go-back-to-previous-client is called")
        val res = IncomeTaxSubscriptionFrontend.submitCannotGoBackToPreviousClient(Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Agent Service Account")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.agentServicesAccountHomeUrl)
        )
      }
    }
    "redirect to Enter Client Details" when {
      "selecting the Reenter Client Details radio button" in {
        val userInput = ReenterClientDetails

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/cannot-go-back-to-previous-client is called")
        val res = IncomeTaxSubscriptionFrontend.submitCannotGoBackToPreviousClient(Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Enter Client Details")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.AddAnotherClientController.addAnother().url)
        )
      }
      "selecting the Sign Up Another Client radio button" in {
        val userInput = SignUpAnotherClient

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/cannot-go-back-to-previous-client is called")
        val res = IncomeTaxSubscriptionFrontend.submitCannotGoBackToPreviousClient(Some(userInput))

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

        When("POST /client/cannot-go-back-to-previous-client is called")
        val res = IncomeTaxSubscriptionFrontend.submitCannotGoBackToPreviousClient(None)

        Then("Should return a SEE_OTHER with a redirect location of Enter Client Details")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.cannot-go-back-previous-client.title") + serviceNameGovUk),
          errorDisplayed()
        )
      }
    }
  }
}


