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
import play.api.http.Status.OK

class NotEnrolledAgentServicesControllerISpec extends ComponentSpecBase {

  "GET /not-enrolled-agent-services " should {
    "show the not enrolled page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /not-enrolled-agent-services is called")
      val res = IncomeTaxSubscriptionFrontend.notEnrolledAgentServices()
      val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
      Then("Should return a OK with the agent not enrolled page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("agent.not-enrolled-agent-services.title") +serviceNameGovUk)
      )
    }
  }
}
