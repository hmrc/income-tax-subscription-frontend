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

package controllers.individual.claimenrolment.spsClaimEnrol


import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.AuthStub
import play.api.http.Status._
import _root_.common.Constants.ITSASessionKeys

class SPSHandoffForClaimEnrolControllerISpec extends ComponentSpecBase {


  s"GET ${controllers.individual.claimenrolment.spsClaimEnrol.routes.SPSHandoffForClaimEnrolController.redirectToSPS.url}" when {

    "the user is not authorised" should {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsHandoff(
          sessionKeys = Map(
            ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
          )
        )

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/claim-enrolment/sps-handoff"))
        )
      }
    }

    "the feature switch SPSEnabled and claim enrolment both set to true" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /claim-enrolment/sps-handoff is called")

      val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsHandoff(
        sessionKeys = Map(
          ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
        )
      )


      Then("Should return a SEE_OTHER and redirect to SPS")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(claimEnrolSpsHandoffURI)
      )
    }
  }

}
