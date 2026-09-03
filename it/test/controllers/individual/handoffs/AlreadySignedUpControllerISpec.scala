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

package controllers.individual.handoffs

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.basGatewaySignOut
import helpers.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}

class AlreadySignedUpControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/already-signed-up" when {
    "the Subscription Details Connector is not applicable" should {
      "show the already enrolled page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubEnrolled()

        When("GET /already-signed-up is called")
        val res = IncomeTaxSubscriptionFrontend.alreadySignedUp()
        val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
        Then("Should return a OK with the already enrolled page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("already-enrolled.title") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/already-signed-up" when {
    "redirect to" should {
      "V&C" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubEnrolled(true)

        When("POST /already-signed-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAlreadySignedUp()

        Then("Should log user out and redirect to BTA/PTA")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI("http://localhost:9081/report-quarterly/income-and-expenses/view")
        )
      }

      "tax account after logout" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubEnrolled(false)

        When("POST /already-signed-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAlreadySignedUp()

        Then("Should log user out and redirect to BTA/PTA")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignOut("http://localhost:9280/account"))
        )
      }
    }
  }
}
