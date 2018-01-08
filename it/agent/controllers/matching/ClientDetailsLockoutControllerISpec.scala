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

package agent.controllers.matching

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants.{clientDetailsURI, testARN}
import _root_.agent.helpers.servicemocks.{AgentLockoutStub, AuthStub}
import _root_.helpers.IntegrationTestConstants.signOutURI
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class ClientDetailsLockoutControllerISpec extends ComponentSpecBase {

  "GET /error/lockout" when {
    "the agent is still locked out" should {
      "show the locked out page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        AgentLockoutStub.stubAgentIsLocked(testARN)

        When("I call GET /error/lockout")
        val res = IncomeTaxSubscriptionFrontend.showClientDetailsLockout()

        Then("The result should have a status of OK")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.client-details-lockout.title"))
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

        Then("The result should have a status of SEE_OTHER")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(clientDetailsURI)
        )
      }
    }
  }

}
