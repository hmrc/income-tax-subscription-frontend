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

import config.featureswitch.FeatureSwitch.ConfirmationPage
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.signOutURI
import helpers.servicemocks.AuthStub
import play.api.http.Status._
import utilities.SubscriptionDataKeys._

class ConfirmationControllerISpec extends ComponentSpecBase {

  "GET /confirmation" when {
    "the user is enrolled and confirm SPS preferences" when {
      "the Sign up confirmation page feature switch is disabled" should {
        "return the sign up complete page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubEnrolled()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
          And("disable the Sign up confirmation page feature switch")
          disable(ConfirmationPage)

          When("GET /confirmation is called")
          val res = IncomeTaxSubscriptionFrontend.confirmation()
          val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
          Then("Should return a OK with the confirmation page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("sign-up-complete.heading") + serviceNameGovUk)
          )
        }
      }

      "the Sign up confirmation page feature switch is enabled" should {
        "return the sign up confirmation page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubEnrolled()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
          And("enable the Sign up confirmation page feature switch")
          enable(ConfirmationPage)

          When("GET /confirmation is called")
          val res = IncomeTaxSubscriptionFrontend.confirmation()
          val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
          Then("Should return a OK with the confirmation page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("sign-up-confirmation.heading") + serviceNameGovUk)
          )
        }
      }
    }

    "the user is not enrolled" should {
      "return a NOT_FOUND" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.confirmation()

        Then("Should return a NOT_FOUND status")
        res must have(
          httpStatus(NOT_FOUND))
      }
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

    "return a NOT_FOUND" when {
      "the user is not enrolled" in {
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

}
