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

import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}

class ClientDetailsErrorControllerISpec extends ComponentSpecBase {

  "GET /error/client-details" should {
    "the user is not authenticated" should {
      "redirect to the login page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubUnauthorised()

        When("GET /error/client-details is called")
        val res = IncomeTaxSubscriptionFrontend.showClientDetailsError()

        Then("Should return a SEE_OTHER with a redirect location of gg sign in")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/error/client-details"))
        )
      }

      "when the user is authenticated" should {
        "show the no matching client page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("GET /error/client-details is called")
          val res = IncomeTaxSubscriptionFrontend.showClientDetailsError()
          val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the no matching client page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.client-details-error.heading") + serviceNameGovUk)
          )
        }
      }
    }
  }
}
