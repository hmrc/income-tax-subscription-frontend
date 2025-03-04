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
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.libs.json.JsString
import play.api.test.Helpers._

class CannotSignUpThisYearControllerISpec extends ComponentSpecBase {

  s"GET ${routes.CannotSignUpThisYearController.show.url}" when {
    "the user is unauthenticated" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.showCannotSignUpThisYear()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/error/cannot-sign-up-for-current-year"))
        )
      }
    }
    "the user is in an incorrect state" must {
      "redirect the user to the correct location" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showCannotSignUpThisYear(hasJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "the user is authenticated and in a confirmed client state" must {
      "return OK with the page content" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        val result = IncomeTaxSubscriptionFrontend.showCannotSignUpThisYear()

        result must have(
          httpStatus(OK),
          pageTitle(s"${messages("agent.cannot-sign-up.title")} - Use software to report your client’s Income Tax - GOV.UK")
        )
      }
    }
  }

  s"POST ${routes.CannotSignUpThisYearController.submit.url}" when {
    "the user is unauthenticated" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitCannotSignUpThisYear()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/error/cannot-sign-up-for-current-year"))
        )
      }
    }
    "the user is in an incorrect state" must {
      "redirect the user to the correct location" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitCannotSignUpThisYear(hasJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "the user is authenticated and in a confirmed client state" must {
      "redirect the user to the using software page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        val result = IncomeTaxSubscriptionFrontend.submitCannotSignUpThisYear()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.UsingSoftwareController.show.url)
        )
      }
    }
  }
}
