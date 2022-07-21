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

import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import helpers.IntegrationTestConstants.claimEnrolmentResolverURI
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status.{OK, SEE_OTHER}
import _root_.common.Constants.ITSASessionKeys

class AddMTDITOverviewControllerISpec extends ComponentSpecBase  with SessionCookieCrumbler {



  "GET /claim-enrolment/overview" should {
    "return the add mtdit overview page" in {

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/overview is called")
        val res = IncomeTaxSubscriptionFrontend.addMTDITOverview()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the AddMTDITOverview page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("mtdit-overview.heading") + serviceNameGovUk)
        )

        getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) must be(Some(ClaimEnrolmentJourney.name))
      }
    }


  "POST /claim-enrolment/overview" should {
      "redirect the user to the claim enrolment resolver" when {
        "all calls are successful" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("POST /claim-enrolment/overview is called")
          val res = IncomeTaxSubscriptionFrontend.submitAddMTDITOverview()

          Then("Should return a SEE_OTHER with a redirect location of the claim enrolment confirmation page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(claimEnrolmentResolverURI)
          )
        }
      }
    }

}
