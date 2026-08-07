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

import _root_.common.Constants.ITSASessionKeys
import auth.individual.ClaimEnrolment as ClaimEnrolmentJourney
import common.Constants.ITSASessionKeys.JourneyStateKey
import connectors.stubs.SessionDataConnectorStub
import connectors.stubs.SessionDataConnectorStub.stubSaveJourneyState
import helpers.IntegrationTestConstants.{IndividualURI, basGatewaySignIn}
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import models.individual.claimenrolment.ClaimEnrolmentOrigin
import models.individual.claimenrolment.ClaimEnrolmentOrigin.*
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.JsString

class AddMTDITOverviewControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  "GET /claim-enrolment/overview" should {
    "redirect the user to log in" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()
        stubSaveJourneyState()(OK)

        val res = IncomeTaxSubscriptionFrontend.addMTDITOverview()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }
    "return the add mtd it overview page" when {
      "a bta origin parameter is provided" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubSaveSessionData[ClaimEnrolmentOrigin](ITSASessionKeys.CLAIM_ENROLMENT_ORIGIN, ClaimEnrolmentBTA)(OK)
        stubSaveJourneyState(ClaimEnrolmentJourney.name)(OK)

        val res = IncomeTaxSubscriptionFrontend.addMTDITOverview(maybeOrigin = Some(ClaimEnrolmentBTA.key))

        res must have(
          httpStatus(OK),
          pageTitle(messages("mtdit-overview.heading", "business tax account") + serviceNameGovUk)
        )
      }
      "a pta origin parameter is provided" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubSaveSessionData[ClaimEnrolmentOrigin](ITSASessionKeys.CLAIM_ENROLMENT_ORIGIN, ClaimEnrolmentPTA)(OK)
        stubSaveJourneyState(ClaimEnrolmentJourney.name)(OK)

        val res = IncomeTaxSubscriptionFrontend.addMTDITOverview(maybeOrigin = Some(ClaimEnrolmentPTA.key))

        res must have(
          httpStatus(OK),
          pageTitle(messages("mtdit-overview.heading", "personal tax account") + serviceNameGovUk)
        )
      }
      "no origin parameter is provided and the claim enrolment origins feature switch is enabled" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubSaveSessionData[ClaimEnrolmentOrigin](ITSASessionKeys.CLAIM_ENROLMENT_ORIGIN, ClaimEnrolmentSignUp)(OK)
        stubSaveJourneyState(ClaimEnrolmentJourney.name)(OK)

        val res = IncomeTaxSubscriptionFrontend.addMTDITOverview(maybeOrigin = None)

        res must have(
          httpStatus(OK),
          pageTitle(messages("mtdit-overview.heading", "online services account") + serviceNameGovUk)
        )
      }
    }

    "return an internal server error" when {
      "there was a failure saving the origin to session" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubSaveSessionData[ClaimEnrolmentOrigin](ITSASessionKeys.CLAIM_ENROLMENT_ORIGIN, ClaimEnrolmentBTA)(INTERNAL_SERVER_ERROR)
        stubSaveJourneyState()(OK)

        val res = IncomeTaxSubscriptionFrontend.addMTDITOverview(maybeOrigin = None)

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  "POST /claim-enrolment/overview" should {
    "redirect the user to the claim enrolment resolver" when {
      "all calls are successful" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        SessionDataConnectorStub.stubGetAllSessionData(Map(
          JourneyStateKey -> JsString(ClaimEnrolmentJourney.name)
        ))

        When("POST /claim-enrolment/overview is called")
        val res = IncomeTaxSubscriptionFrontend.submitAddMTDITOverview()

        Then("Should return a SEE_OTHER with a redirect location of the claim enrolment confirmation page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.claimEnrolmentResolverURI)
        )
      }
    }
  }

}
