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

package incometax.business

import core.auth.{Registration, SignUp}
import core.config.featureswitch
import core.config.featureswitch.FeatureSwitching
import core.services.CacheConstants.BusinessAddress
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AddressLookupStub, AuthStub, KeystoreStub}
import play.api.http.Status._
import play.api.i18n.Messages

class BusinessAddressControllerISpec extends ComponentSpecBase with FeatureSwitching {

  // TODO remove this when registration is enabled by default
  enable(featureswitch.RegistrationFeature)

  "GET /report-quarterly/income-and-expenses/sign-up/business/address" when {

    "There is no address in keystore" should {
      "redirect to business address init" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddress(Registration)

        Then(s"return a SEE_OTHER with a redirect location of $businessAddressInitURI")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAddressInitURI)
        )
      }
    }

    "There is an address in keystore" should {
      "show the business address page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddress(Registration)

        Then(s"return a OK with the business address page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("business.address.title"))
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

  "POST /report-quarterly/income-and-expenses/sign-up/business/address" when {

    "when not in edit mode" should {
      "redirect to accounting period dates" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AddressLookupStub.stubAddressSuccess()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessAddress(editMode = false, Registration)

        Then(s"return a SEE_OTHER with a redirect location of $businessStartDateURI")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessStartDateURI)
        )
      }
    }

    "when in edit mode" should {
      "redirect to business address init" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AddressLookupStub.stubAddressSuccess()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessAddress(editMode = true, Registration)

        Then(s"return a SEE_OTHER with a redirect location of $businessAddressInitURI")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAddressInitURI)
        )
      }
    }

    "state not in Registration" should {
      "not show the business address page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /business/address is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessAddress(editMode = false, SignUp)

        Then("return a redirect location of indexURI")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(indexURI)
        )

        When("GET /business/address?editMode=true is called")
        val editRes = IncomeTaxSubscriptionFrontend.submitBusinessAddress(editMode = true, SignUp)

        Then("return a redirect location of indexURI")
        editRes should have(
          httpStatus(SEE_OTHER),
          redirectURI(indexURI)
        )
      }
    }
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/address/init" when {

    "call request successful" should {
      "show the business address page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AddressLookupStub.stubAddressSuccess()

        When("GET /business/address/init is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddressInit(Registration)

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

        When("GET /business/address/init is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddressInit(Registration)

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

        When("GET /business/address/init is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddressInit(SignUp)

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
        val res = IncomeTaxSubscriptionFrontend.businessAddressCallback(editMode = false, Registration)

        Then(s"return a SEE_OTHER with a redirect location of $businessStartDateURI")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessStartDateURI)
        )
      }
    }

    "call request not successful" should {
      "not redirect" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        AddressLookupStub.stubAddressFetchFailure()

        When("GET /business/address/callback is called")
        val res = IncomeTaxSubscriptionFrontend.businessAddressCallback(editMode = false, Registration)

        Then("return an internal server error")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

}
