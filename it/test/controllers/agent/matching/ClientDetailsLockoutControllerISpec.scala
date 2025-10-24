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

import helpers.IntegrationTestConstants.{AgentURI, basGatewaySignIn, testARN}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.{AgentLockoutStub, AuthStub}
import play.api.http.Status.{OK, SEE_OTHER}

class ClientDetailsLockoutControllerISpec extends ComponentSpecBase {

  "GET /error/lockout" when {
    "the agent is not authorised" should {
      "redirect to login page" in {
        AuthStub.stubUnauthorised()
        val res = IncomeTaxSubscriptionFrontend.showClientDetailsLockout()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/error/lockout"))
        )
      }
    }
    "the agent is authorised" when {
      "the agent is still locked out" should {
        "show the locked out page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          AgentLockoutStub.stubAgentIsLocked(testARN)

          When("I call GET /error/lockout")
          val res = IncomeTaxSubscriptionFrontend.showClientDetailsLockout()
          val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
          Then("The result must have a status of OK")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.client-details-lockout.title") + serviceNameGovUk)
          )
        }
      }

      "the agent is no longer locked out" should {
        "show the client details page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          AgentLockoutStub.stubAgentIsNotLocked(testARN)

          When("I call GET /error/lockout")
          val res = IncomeTaxSubscriptionFrontend.showClientDetailsLockout()

          Then("The result must have a status of SEE_OTHER")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.clientDetailsURI)
          )
        }
      }
    }
  }

}
