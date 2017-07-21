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
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.i18n.Messages
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, SubscriptionStub}

class HomeControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up" when {

    "feature-switch.show-guidance is true" should {
      "return the guidance page" in {
        When("We hit to the guidance page route")
        val res = IncomeTaxSubscriptionFrontend.startPage()

        Then("Return the guidance page")
        res.status shouldBe Status.OK
        val document = Jsoup.parse(res.body)

        document.title shouldBe Messages("frontpage.title")
      }
    }
  }

  "GET /report-quarterly/income-and-expenses/sign-up/index" when {

    "keystore not applicable" should {
      "redirect to the claim subscription page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SubscriptionStub.stubGetSubscriptionFound()

        When("GET /index is called")
        val res = IncomeTaxSubscriptionFrontend.indexPage()

        Then("Should return a SEE OTHER with the claim subscription page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(claimSubscriptionURI)
        )
      }
    }
  }

}
