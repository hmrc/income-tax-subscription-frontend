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
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub, SubscriptionStub}
import play.api.http.Status._
import play.api.i18n.Messages
import services.CacheConstants._
import helpers.IntegrationTestConstants._


class CheckYourAnswersControllerISpec extends ComponentSpecBase{
  "GET /report-quarterly/income-and-expenses/sign-up/check-your-answers" when {
    "keystore returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("summary.title"))
        )
      }
    }

    "keystore does not return the terms field" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreData(fullKeystoreData - Terms)

      When("GET /check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of terms")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(termsURI)
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/check-your-answers" when {
    "keystore returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        SubscriptionStub.stubSuccessfulSubscription()
        KeystoreStub.stubPutMtditId()

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of confirmation")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmationURI)
        )
      }
    }

    "keystore does not return the terms field" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreData(fullKeystoreData - Terms)

      When("POST /check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of terms")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(termsURI)
      )
    }
  }
}
