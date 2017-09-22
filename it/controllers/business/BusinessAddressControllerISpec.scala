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

package controllers.business

import auth.{Registration, SignUp}
import helpers.ComponentSpecBase
import helpers.servicemocks.{AddressLookupStub, AuthStub, KeystoreStub}
import play.api.http.Status._
import helpers.IntegrationTestConstants._
import services.CacheConstants.BusinessAddress

class BusinessAddressControllerISpec extends ComponentSpecBase {

  // TODO remove this when registration is enabled by default
  override def config: Map[String, String] = super.config.+("feature-switch.enable-registration" -> "true")

  "GET /report-quarterly/income-and-expenses/sign-up/business/address" when {

    "call request successful" should {
      "show the business address page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AddressLookupStub.stubAddressSuccess()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddress(Registration)

        Then(s"return a SEE_OTHER with a redirect location of $testUrl")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(testUrl)
        )
      }
    }

    "call request not successful" should {
      "not show the business address page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AddressLookupStub.stubAddressFailure()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddress(Registration)

        Then("return an internal server error")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "state not in Registration" should {
      "not show the business address page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddress(SignUp)

        Then("return a redirect location of indexURI")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(indexURI)
        )
      }
    }
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/address/callback" when {

    "call request successful" should {
      "redirect to business start date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AddressLookupStub.stubAddressFetchSuccess()
        KeystoreStub.stubKeystoreSave(BusinessAddress)

        When("GET /business/address/callback is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddressCallback(Registration)

        Then(s"return a SEE_OTHER with a redirect location of $businessStartDateURI")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessStartDateURI)
        )
      }
    }



  }
}