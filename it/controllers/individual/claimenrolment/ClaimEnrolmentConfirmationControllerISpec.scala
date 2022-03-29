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

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class ClaimEnrolmentConfirmationControllerISpec extends ComponentSpecBase {


  "GET /claim-enrolment/confirmation" should {
    "return the confirmation page when the user is enrolled and enrolment claimed" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentConfirmation()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the confirmation page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("claimConfirm.title") + serviceNameGovUk)
        )
      }
    }

  "POST /claim-enrolment/confirmation" should {
    "redirect to bta" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /claim-enrolment/confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.continueClaimEnrolmentJourneyConfirmation()
        Then("Should return a SEE_OTHER going to bta")
        res must have(
          httpStatus(SEE_OTHER), redirectURI("https://www.tax.service.gov.uk/business-account")
        )
      }
    }

}
