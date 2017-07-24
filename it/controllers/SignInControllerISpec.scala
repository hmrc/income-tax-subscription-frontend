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

package controllers

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.AuthStub
import play.api.http.Status.SEE_OTHER

class SignInControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/sign-in" when {

    "keystore not applicable" should {
      "show the sign in page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /sign-in is called")
        val res = IncomeTaxSubscriptionFrontend.signIn()

        Then("Should return a SEE_OTHER with a redirect location of gg sign in")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(ggSignInURI)
        )
      }
    }
  }
}
