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

package controllers.individual.claimenrolment

import config.featureswitch.FeatureSwitch.ClaimEnrolment
import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class ClaimEnrolmentConfirmationControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    super.beforeEach()
  }

  "GET /claim-enrolment/confirmation" should {
    "return the confirmation page when the user is enrolled and enrolment claimed" when {
      "the claim enrolment feature switch is enabled" in {
        enable(ClaimEnrolment)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentConfirmation()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the confirmation page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("claimConfirm.title") + serviceNameGovUk)
        )
      }
    }
    "return a not found page" when {
      "the claim enrolment feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentConfirmation()

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "POST /claim-enrolment/confirmation" should {
    "redirect to bta" when {
      "the claim enrolment feature switch is enabled" in {
        enable(ClaimEnrolment)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /claim-enrolment/confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.continueClaimEnrolmentJourneyConfirmation()
        Then("Should return a SEE_OTHER going to bta")
        res should have(
          httpStatus(SEE_OTHER), redirectURI("https://www.tax.service.gov.uk/business-account")
        )
      }
    }
    "return a not found page" when {
      "the claim enrolment feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.continueClaimEnrolmentJourneyConfirmation()

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

}