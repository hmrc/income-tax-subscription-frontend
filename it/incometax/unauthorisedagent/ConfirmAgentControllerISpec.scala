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

package incometax.unauthorisedagent

import core.config.featureswitch.UnauthorisedAgentFeature
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks._
import play.api.http.Status._
import play.api.i18n.Messages

class ConfirmAgentControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/confirm-agent" when {
    "the unauthorised agent feature switch is enabled" should {
      "return the confirm agent page" in {
        Given("The feature switch is on")
        enable(UnauthorisedAgentFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AgencyNameStub.stubAgencyName()

        When("GET /confirm-agent is called")
        val res = IncomeTaxSubscriptionFrontend.confirmAgent()

        Then("Should return an OK with the confirm agent page")
        res should have(
          httpStatus(OK)
          //TODO - Add in again when agent services work is completed
//          pageTitle(Messages("confirm-agent.title", testAgencyName))
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/confirm-agent" when {
    "the unauthorised agent feature switch is enabled" should {
      "when the user answered yes should redirects to authorise agent page" in {
        Given("The feature switch is on")
        enable(UnauthorisedAgentFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST confirm-agent is called")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmAgent(testConfirmAgentYes)

        Then("Should return a SEE_OTHER with a redirect location of ")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(authoriseAgentUri)
        )
      }

      "when the user answered no should redirects to agent not authorised page" in {
        Given("The feature switch is on")
        enable(UnauthorisedAgentFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST confirm-agent is called")
        val res = IncomeTaxSubscriptionFrontend.submitConfirmAgent(testConfirmAgentNo)

        Then("Should return a SEE_OTHER with a redirect location of ")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(agentNotAuthorisedUri)
        )
      }
    }
  }
}

