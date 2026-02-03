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

package controllers.agent

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import models.{EligibilityStatus, No, Yes, YesNo}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.WSResponse
import utilities.agent.TestConstants.testUtr

class UsingSoftwareControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${controllers.agent.routes.UsingSoftwareController.show().url}" when {

    "the user is unauthenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.showUsingSoftware()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/using-software"))
        )
      }
    }
  }

  s"POST ${controllers.agent.routes.UsingSoftwareController.submit().url}" should {
    "return a redirect to the login page" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitUsingSoftware()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/using-software"))
        )
      }
    }

    s"return a redirect to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" when {
      "the user presses the Continue button" in {
        Given("I am authenticated")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        When(s"POST ${controllers.agent.routes.UsingSoftwareController.submit().url} is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitUsingSoftware()

        Then("Should return SEE_OTHER to the What Year To Sign Up Controller")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.ClientDetailsController.show().url)
        )
      }
    }
  }
}















