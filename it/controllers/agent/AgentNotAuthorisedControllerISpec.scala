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

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.servicemocks.AuthStub
import agent.helpers.IntegrationTestConstants.indexURI
import core.config.featureswitch
import core.config.featureswitch.FeatureSwitching
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class AgentNotAuthorisedControllerISpec extends ComponentSpecBase with FeatureSwitching{

  enable(featureswitch.UnauthorisedAgentFeature)

  "GET /error/not-authorised" should {
    "show the agent not authorised page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/not-authorised is called")
      val res = IncomeTaxSubscriptionFrontend.agentNotAuthorised()

      Then("Should return a OK with the agent not authorised page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("agent.not-authorised-error.title"))
      )
    }
  }

  "POST /error/not-authorised" when {

    "not in edit mode" should {

      "select the Continue button on the agent not authorised page" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /error/not-authorised")
        val res = IncomeTaxSubscriptionFrontend.submitAgentNotAuthorised()

        Then("Should return a SEE_OTHER with a redirect location of home")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(indexURI)
        )

      }

    }
  }
}
