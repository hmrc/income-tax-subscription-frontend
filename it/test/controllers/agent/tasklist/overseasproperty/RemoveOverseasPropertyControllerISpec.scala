/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent.tasklist.overseasproperty

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants.AgentURI
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status._
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys.OverseasProperty

class RemoveOverseasPropertyControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/client/business/remove-overseas-property-business" when {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET client/business/remove-overseas-property-business is called")
      val res = IncomeTaxSubscriptionFrontend.getRemoveClientOverseasProperty
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      Then("Should return a OK with the client remove Overseas property confirmation page displaying")
      res must have(
        httpStatus(OK),
        pageTitle(messages("agent.remove-overseas-property-business.heading") + serviceNameGovUk)
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/remove-overseas-property-business" should {
    "redirect to the client task list page" when {
      "the user submits the 'yes' answer" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(OverseasProperty)
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)

        When("POST client/business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("Yes")))

        Then("Should return a SEE_OTHER with a redirect location of client task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(1))
        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation, Some(1))
      }

      "the user submits the 'no' answer" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST client/business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("No")))

        Then("Should return a SEE_OTHER with a redirect location of client task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
      }
    }

    "return a BAD_REQUEST" when {
      "no option was selected on the client remove Overseas property page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("")))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "cannot remove overseas property" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetailsFailure(OverseasProperty)

        When("POST /business/remove-uk-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("Yes")))

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}

