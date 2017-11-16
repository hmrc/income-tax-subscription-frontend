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

package agent.controllers

import _root_.agent.helpers.IntegrationTestConstants.{testMTDID, _}
import _root_.agent.helpers.IntegrationTestModels.fullKeystoreData
import _root_.agent.helpers.servicemocks._
import _root_.agent.helpers.{ComponentSpecBase, SessionCookieCrumbler}
import _root_.agent.services.CacheConstants._
import helpers.IntegrationTestConstants
import helpers.servicemocks.SubscriptionStub
import play.api.http.Status._
import play.api.i18n.Messages

class CheckYourAnswersControllerISpec extends ComponentSpecBase {

  "GET /check-your-answers" when {
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
          pageTitle(Messages("agent.summary.title"))
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

    "redirect to sign-in when auth fails" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("GET /check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(IntegrationTestConstants.ggSignInURI)
      )
    }
  }


  "POST /check-your-answers" when {
    "The whole subscription process was successful" should {
      "call subscription on the back end service and redirect to confirmation page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        SubscriptionStub.stubSuccessfulSubscription(checkYourAnswersURI)
        GGAdminStub.stubKnowFactsSuccess(testNino, testMTDID)
        KeystoreStub.stubPutMtditId()

        When("I call POST /check-your-answers")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmationURI)
        )

        val cookieMap = SessionCookieCrumbler.getSessionMap(res)
        cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

        GGAdminStub.verifyKnownFacts(testNino, testMTDID, Some(1))
      }
    }

    "The whole subscription process was unsuccessful" should {

      "show an error page when subscription failed" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("I call POST /check-your-answers")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("The result show the error page")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        val cookieMap = SessionCookieCrumbler.getSessionMap(res)
        cookieMap.get(ITSASessionKeys.MTDITID) shouldBe None

        GGAdminStub.verifyKnownFacts(testNino, testMTDID, Some(0))
      }

      "show an error page when add known facts failed" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        SubscriptionStub.stubSuccessfulSubscription(checkYourAnswersURI)
        GGAdminStub.stubKnowFactsFailure(testNino, testMTDID)

        When("I call POST /check-your-answers")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("The result show the error page")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        val cookieMap = SessionCookieCrumbler.getSessionMap(res)
        cookieMap.get(ITSASessionKeys.MTDITID) shouldBe None

        GGAdminStub.verifyKnownFacts(testNino, testMTDID, Some(1))
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

    "redirect to sign-in when auth fails" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("GET /check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(IntegrationTestConstants.ggSignInURI)
      )
    }

    "return an INTERNAL_SERVER_ERROR when the backend service returns a NOT_FOUND" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubFullKeystore()
      SubscriptionStub.stubCreateSubscriptionNotFound(checkYourAnswersURI)

      When("POST /check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

      Then("Should return an INTERNAL_SERVER_ERROR")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }
  }

}
