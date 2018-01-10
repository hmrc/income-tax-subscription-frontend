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
import helpers.servicemocks._
import play.api.http.Status._
import play.api.i18n.Messages
import helpers.IntegrationTestConstants._

class AgentNotAuthorisedControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/agent-not-authorised " when {
    "the unauthorised agent feature switch is enabled" should {
      "return the agent not authorised page" in {
        Given("The feature switch is on")
        enable(UnauthorisedAgentFeature)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SubscriptionStoreStub.stubSuccessfulDeletion()

        When("GET /agent-not-authorised  is called")
        val res = IncomeTaxSubscriptionFrontend.agentNotAuthorised()

        Then("Should return an OK with the agent not authorised page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent-not-authorised.title", testAgencyName))
        )
      }
    }
  }
}

