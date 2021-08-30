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

import config.featureswitch.FeatureSwitch.{ClaimEnrolment, SPSEnabled}
import config.featureswitch.FeatureSwitching
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.AuthStub
import play.api.http.Status.{SEE_OTHER, _}
import utilities.ITSASessionKeys
import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}

class SPSHandoffForClaimEnrolControllerISpec extends ComponentSpecBase with FeatureSwitching {


  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(SPSEnabled)
    disable(ClaimEnrolment)
  }

  s"GET ${controllers.individual.claimenrolment.spsClaimEnrol.routes.SPSHandoffForClaimEnrolController.redirectToSPS().url}" when {

    "the user is not authorised" should {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsHandoff(
          sessionKeys = Map(
            ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
          )
        )

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/claim-enrolment/sps-handoff"))
        )
      }
    }

    "the feature switch SPSEnabled and claim enrolment both set to true" in {
      Given("I setup the Wiremock stubs")
      enable(SPSEnabled)
      enable(ClaimEnrolment)
      AuthStub.stubAuthSuccess()

      When("GET /claim-enrolment/sps-handoff is called")

      val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsHandoff(
        sessionKeys = Map(
          ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
        )
      )


      Then("Should return a SEE_OTHER and redirect to SPS")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(claimEnrolSpsHandoffURI)
      )
    }

    "the SPSEnabled and claim enrolment feature switch are both set to false" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /claim-enrolment/sps-handoff is called")
      val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsHandoff(
        sessionKeys = Map(
          ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
        )
      )

      Then("Should return a not found page to the user")
      res should have(
        httpStatus(NOT_FOUND),
        pageTitle("Page not found - 404")
      )
    }

    "the feature switch SPSEnabled set to false and claim enrolment feature switch set to true" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      enable(ClaimEnrolment)

      When("GET /claim-enrolment/sps-handoff is called")
      val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsHandoff(
        sessionKeys = Map(
          ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
        )
      )

      Then("Should return a not found page to the user")
      res should have(
        httpStatus(NOT_FOUND),
        pageTitle("Page not found - 404")
      )
    }

    "the feature switch SPSEnabled set to true and claim enrolment feature switch set to false" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      enable(SPSEnabled)

      When("GET /claim-enrolment/sps-handoff is called")
      val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsHandoff(
        sessionKeys = Map(
          ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
        )
      )

      Then("Should return a not found page to the user")
      res should have(
        httpStatus(NOT_FOUND),
        pageTitle("Page not found - 404")
      )
    }

  }

}
