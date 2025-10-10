/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent.eligibility

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.libs.json.JsString
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utilities.SubscriptionDataKeys

class ClientCanSignUpControllerISpec extends ComponentSpecBase {

  "GET /client/can-sign-up" should {
    "return a status of OK" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

      When("GET /client/can-sign-up is called")
      val result: WSResponse = IncomeTaxSubscriptionFrontend.showCanSignUp()

      Then("Should return a OK")
      result must have(
        httpStatus(OK),
        httpContentType(HTML)
      )
    }

    "the user is unauthenticated" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.showCanSignUp()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/can-sign-up"))
        )
      }
    }

    "the user is in an incorrect state" must {
      "redirect the user to the correct location" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showCanSignUp(hasJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
  }

  "POST /client/can-sign-up" when {
    "the user clicks sign up this client button" should {
      s"return a redirect to ${controllers.agent.routes.UsingSoftwareController.show(false).url}" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](
          id = SubscriptionDataKeys.EligibilityInterruptPassed,
          body = true
        )

        When("POST /client/can-sign-up is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCanSignUp()

        Then("Should return SEE_OTHER to the Using Software Controller")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.UsingSoftwareController.show(false).url)
        )
      }
    }

    "An error is returned when saving eligibility interrupt passed flag" should {
      "return an internal server error" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(
          id = SubscriptionDataKeys.EligibilityInterruptPassed
        )

        When("POST /client/can-sign-up is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCanSignUp()

        Then("Should return SEE_OTHER to the WhatYouNeedToDoController")

        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "the user is unauthenticated" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitCanSignUp()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/can-sign-up"))
        )
      }
    }
    "the user is in an incorrect state" must {
      "redirect the user to the correct location" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitCanSignUp(hasJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
  }

}
