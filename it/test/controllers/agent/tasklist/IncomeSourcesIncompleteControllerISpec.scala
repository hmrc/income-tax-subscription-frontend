/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent.tasklist

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import utilities.agent.TestConstants.testUtr

class IncomeSourcesIncompleteControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = "Use software to report your client’s Income Tax - GOV.UK"

  s"GET ${controllers.agent.tasklist.routes.IncomeSourcesIncompleteController.show.url}" when {
    "the user is unauthenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.showIncomeSourcesIncomplete()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/income-sources-incomplete"))
        )
      }
    }
    "the user does not have any journey state" should {
      "redirect to the cannot go back page" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showIncomeSourcesIncomplete(hasJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "the user is authenticated and in a confirmed client state" should {
      "return OK with the page content" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        val result = IncomeTaxSubscriptionFrontend.showIncomeSourcesIncomplete()

        result must have(
          httpStatus(OK),
          pageTitle(s"${messages("agent.income-sources-incomplete.heading")} - $serviceNameGovUk")
        )
      }
    }
  }

  s"POST ${controllers.agent.tasklist.routes.IncomeSourcesIncompleteController.submit.url}" when {
    "the user is unauthenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.submitIncomeSourcesIncomplete()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/income-sources-incomplete"))
        )
      }
    }
    "the user does not have any journey state" should {
      "redirect to the cannot go back page" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitIncomeSourcesIncomplete(hasJourneyState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "the user is authenticated and in a confirmed client state" should {
      "redirect the user to the your income sources page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        val result = IncomeTaxSubscriptionFrontend.submitIncomeSourcesIncomplete()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        )
      }
    }
  }

}
