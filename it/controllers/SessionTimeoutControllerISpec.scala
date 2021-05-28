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

package controllers

import auth.agent.AgentUserMatched
import controllers.Assets.SEE_OTHER
import controllers.agent.ITSASessionKeys
import helpers.CustomMatchers
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.IntegrationTestConstants.{testNino, testUrl, testUtr}
import helpers.servicemocks.AuthStub
import play.api.http.Status.OK

class SessionTimeoutControllerISpec extends ComponentSpecBase with SessionCookieCrumbler with CustomMatchers {

  "GET /report-quarterly/income-and-expenses/sign-up/session-timeout" when {

    "the Subscription Details Connector is not applicable" should {
      "show the session timeout page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /session-timeout is called")
        val res = IncomeTaxSubscriptionFrontend.sessionTimeout()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the session timeout page")
        res should have(
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
        val sessionMap = Map(
          ITSASessionKeys.NINO -> testNino,
          ITSASessionKeys.UTR -> testUtr)
        val res = IncomeTaxSubscriptionFrontend.keepAlive(sessionMap)
        val session = getSessionMap(res)
        session.get(ITSASessionKeys.NINO) shouldBe Some(testNino)
        session.get(ITSASessionKeys.UTR) shouldBe Some(testUtr)
        res should have(
          httpStatus(OK)
        )
      }
    }
  }

  "GET /report-quarterly/income-and-expenses/sign-up/timeout" when {
    "a user times out" should {
      "redirect and sign out the user" in {
        AuthStub.stubAuthSuccess()
        val sessionMap = Map(
          ITSASessionKeys.NINO -> testNino,
          ITSASessionKeys.UTR -> testUtr)

        val res = IncomeTaxSubscriptionFrontend.timeout(sessionMap)
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI("/gg/sign-in?continue=%2Freport-quarterly%2Fincome-and-expenses%2Fsign-up&origin=income-tax-subscription-frontend")
        )
        val session = getSessionMap(res)
        session.keys shouldNot contain(ITSASessionKeys.NINO)
        session.keys shouldNot contain(testNino)
      }
    }
  }

}
