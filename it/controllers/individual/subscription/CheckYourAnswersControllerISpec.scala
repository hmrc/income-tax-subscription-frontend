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

package controllers.individual.subscription

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks._
import play.api.http.Status._
import play.api.i18n.Messages

class CheckYourAnswersControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/check-your-answers" when {
    "keystore returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothV2()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("summary.title"))
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/check-your-answers" when {

    " call the enrolment store successfully" should {
      "successfully send the correct details to the backend for a user with business and property income" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothV2()
        SubscriptionStub.stubSuccessfulSubscriptionV2WithBoth(checkYourAnswersURI)
        TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
        TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
        KeystoreStub.stubPutMtditId()

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()


        Then("Should return a SEE_OTHER with a redirect location of confirmation")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmationURI)
        )
      }

      "successfully send the correct details to the backend for a user with property income" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystorePropertyV2()
        SubscriptionStub.stubSuccessfulSubscriptionV2WithProperty(checkYourAnswersURI)
        TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
        TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
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

    "Refresh Profile call fails" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()
        SubscriptionStub.stubSuccessfulSubscriptionV2WithBoth(checkYourAnswersURI)
        TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
        TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return an INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "Known Facts call fails" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()
        SubscriptionStub.stubSuccessfulSubscriptionV2WithProperty(checkYourAnswersURI)
        TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, BAD_REQUEST)
        TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, BAD_REQUEST)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return an INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "enrolment failure occurs where not on whitelist" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()
        SubscriptionStub.stubSuccessfulSubscriptionV2WithBoth(checkYourAnswersURI)
        TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, FORBIDDEN)
        TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return a INTERNAL SERVER ERROR status")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "enrolment failure occurs where missing details" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()
        SubscriptionStub.stubSuccessfulSubscriptionV2WithBoth(checkYourAnswersURI)
        TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, BAD_REQUEST)
        TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return a INTERNAL SERVER ERROR status")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "enrolment failure occurs where auth success but access error with gateway token" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreFailure()
        SubscriptionStub.stubSuccessfulSubscriptionV2WithBoth(checkYourAnswersURI)
        TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, INTERNAL_SERVER_ERROR)
        TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return a INTERNAL SERVER ERROR status")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "return an INTERNAL_SERVER_ERROR when the backend service returns a NOT_FOUND" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreFailure()
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
