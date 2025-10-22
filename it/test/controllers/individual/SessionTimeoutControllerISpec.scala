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

package controllers.individual

import helpers.IntegrationTestConstants.testNino
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, CustomMatchers, SessionCookieCrumbler}
import play.api.http.Status.{OK, SEE_OTHER}

class SessionTimeoutControllerISpec extends ComponentSpecBase with SessionCookieCrumbler with CustomMatchers {

  "GET /report-quarterly/income-and-expenses/sign-up/session-timeout" when {

    "the Subscription Details Connector is not applicable" should {
      "show the session timeout page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /session-timeout is called")
        val res = IncomeTaxSubscriptionFrontend.sessionTimeout()
        val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
        Then("Should return a OK with the session timeout page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("timeout.title") + serviceNameGovUk)
        )
      }
    }
  }

  "GET /report-quarterly/income-and-expenses/sign-up/keep-alive" when {
    "a  user chooses to not time out" should {
      "return an OK and keep the session" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.keepAlive()

        res must have(
          httpStatus(OK)
        )
      }
    }
  }

  "GET /report-quarterly/income-and-expenses/sign-up/timeout" when {
    "a user times out" should {
      "redirect and sign out the user" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.timeout()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI("http://localhost:9553/bas-gateway/sign-in?continue_url=%2Freport-quarterly%2Fincome-and-expenses%2Fsign-up&origin=income-tax-subscription-frontend")
        )
        val session = getSessionMap(res)
        session.keys mustNot contain(testNino)
      }
    }
  }

}
