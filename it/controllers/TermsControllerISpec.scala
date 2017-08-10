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
import helpers.servicemocks.{AuthStub, KeystoreStub}
import play.api.http.Status._
import play.api.i18n.Messages
import services.CacheConstants

class TermsControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/terms" when {

    "keystore call fails" should {
      "return internal server error" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()

        When("GET /terms is called")
        val res = IncomeTaxSubscriptionFrontend.terms()

        Then("Should return an internal server error")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "keystore returns all data" should {
      "show the terms page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /terms is called")
        val res = IncomeTaxSubscriptionFrontend.terms()

        Then("Should return a OK with the other income page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("terms.title"))
        )
      }
    }

    "redirect to sign-in when auth fails" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("GET /terms is called")
      val res = IncomeTaxSubscriptionFrontend.terms()

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(signInURI)
      )
    }

  }


  "POST /report-quarterly/income-and-expenses/sign-up/terms" when {

    "not in edit mode" should {

      "select the Continue button on the terms page" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        KeystoreStub.stubKeystoreSave(CacheConstants.Terms,true)

        When("POST /terms is called")
        val res = IncomeTaxSubscriptionFrontend.submitTerms()

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "keystore call fails" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()

        When("POST /terms is called")
        val res = IncomeTaxSubscriptionFrontend.submitTerms()

        Then("should return an internal server error")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "redirect to sign-in when auth fails" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubUnauthorised()

        When("POST /terms is called")
        val res = IncomeTaxSubscriptionFrontend.submitTerms()

        Then("Should return a SEE_OTHER with a redirect location of sign-in")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(signInURI)
        )
      }
    }
  }
}
