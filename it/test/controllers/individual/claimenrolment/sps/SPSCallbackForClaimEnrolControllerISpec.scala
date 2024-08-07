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

package controllers.individual.claimenrolment.sps

import _root_.common.Constants.ITSASessionKeys
import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import connectors.stubs.SessionDataConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{IndividualURI, basGatewaySignIn, testNino}
import helpers.WiremockHelper.verifyPost
import helpers.servicemocks.{AuthStub, SubscriptionStub}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}

class SPSCallbackForClaimEnrolControllerISpec extends ComponentSpecBase {


  s"GET ${controllers.individual.claimenrolment.sps.routes.SPSCallbackForClaimEnrolController.callback.url}" when {

    "the user is not authorised" should {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsCallback(hasEntityId = true)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/claim-enrolment/sps-callback"))
        )
      }
    }

    "there is an entityId" when {
      "mtditid retrieves successfully" should {
        "link user's enrolment id to SPS and redirect the user to the Claim Enrolment Confirmation page" in {
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

          val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsCallback(
            hasEntityId = true,
            sessionKeys = Map(
              ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
            )
          )
          verifyPost("/channel-preferences/confirm", count = Some(1))
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.claimEnrolmentConfirmationURI)
          )
        }
      }

      "mtditid retrieves failed" should {
        "throw InternalServerException" in {
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetNoSubscription()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

          val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsCallback(
            hasEntityId = true,
            sessionKeys = Map(
              ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
            )
          )

          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
    "there is no entityId" should {
      "redirect the user to the Claim Enrolment Confirmation page" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.claimEnrolSpsCallback(hasEntityId = false,
          sessionKeys = Map(
            ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name
          )
        )
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.claimEnrolmentConfirmationURI)
        )
      }
    }
  }
}

