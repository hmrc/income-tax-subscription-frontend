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

package controllers.agent.tasklist.selfemployment

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants.AgentURI
import helpers.IntegrationTestModels.testBusinesses
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.{No, Yes}
import play.api.http.Status._
import utilities.SubscriptionDataKeys.SoleTraderBusinessesKey

class RemoveSelfEmploymentBusinessControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/client/business/remove-business" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))

      When("GET business/remove-business is called")
      val res = IncomeTaxSubscriptionFrontend.getRemoveBusiness()

      Then("Should return OK with the remove business page")
      res must have(
        httpStatus(OK),
        pageTitle(
          "Are you sure you want to delete test business - test trade? - Use software to report your client’s Income Tax - GOV.UK"
        )
      )
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the Sole trader business cannot be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))

        When("GET business/remove-business is called")
        val res = IncomeTaxSubscriptionFrontend.getRemoveBusiness(id = "unknown")

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/remove-business" should {
    "redirect to the task list page" when {
      "the user submits the 'yes' answer" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
        IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinessDetails(Seq.empty, None)
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(SoleTraderBusinessesKey)

        When("POST business/remove-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveBusiness(Some(Yes))

        Then("Should return a SEE_OTHER with a redirect location of task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )
      }

      "the user submits the 'no' answer" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))

        When("POST business/remove-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveBusiness(Some(No))

        Then("Should return a SEE_OTHER with a redirect location of task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.taskListURI)
        )
      }

    }

    "return BAD_REQUEST" when {
      "invalid data is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))

        When("POST business/remove-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveBusiness(request = None)

        Then("Should return BAD_REQUEST")
        res must have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }
}
