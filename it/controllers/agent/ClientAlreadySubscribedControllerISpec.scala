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

import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}

class ClientAlreadySubscribedControllerISpec extends ComponentSpecBase {

  "GET /error/client-already-subscribed" should {
    "show the already subscribed page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/client-already-subscribed is called")
      val res = IncomeTaxSubscriptionFrontend.clientAlreadySubscribed()
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      Then("Should return a OK with the client already subscribed page")
      res should have(
        httpStatus(OK),
        pageTitle(messages("agent.client-already-subscribed.title") + serviceNameGovUk)
      )
    }
  }

  "POST /error/client-already-subscribed" should {
    "show the already subscribed page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("POST /error/client-already-subscribed is called")
      val res = IncomeTaxSubscriptionFrontend.submitClientAlreadySubscribed()

      Then("Should return a redirect to client matching")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(clientDetailsURI)
      )
    }
  }

}
