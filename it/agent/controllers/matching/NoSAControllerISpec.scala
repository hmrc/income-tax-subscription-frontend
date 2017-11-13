/*
 * Copyright 2017 HM Revenue & Customs
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

import agent.helpers.ComponentSpecBase
import agent.helpers.IntegrationTestConstants.signOutURI
import agent.helpers.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class NoSAControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/register-for-SA" when {

    "keystore not applicable" should {
      "show the error maintenance page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /register-for-SA is called")
        val res = IncomeTaxSubscriptionFrontend.noSA()

        Then("Should return a OK with the error main income page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.no-sa.title"))
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/register-for-SA" when {

    "always" should {
      "proceed to sign out" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /register-for-SA is called")
        val res = IncomeTaxSubscriptionFrontend.submitNoSA()

        Then("Should return a SEE_OTHER with a redirect location of sign out")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(signOutURI)
        )
      }
    }
  }

}
