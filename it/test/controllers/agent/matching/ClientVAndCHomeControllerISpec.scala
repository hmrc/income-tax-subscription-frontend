/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.agent.matching

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testMtdId, testNino, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.{AuthStub, IncomeTaxSessionDataStub}
import play.api.http.Status.*
import play.api.libs.json.JsString

class ClientVAndCHomeControllerISpec extends ComponentSpecBase {

  s"GET ${appConfig.getVAndCUrl}" should {

    "redirect to the login page" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.handOffVAndC()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/hand-offs/view-and-change"))
        )
      }
    }

    "redirect to view and change" when {
      "all session data is present and the connector returns a success response" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.MTDITID -> JsString(testMtdId),
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        IncomeTaxSessionDataStub.stubSetupViewAndChangeData(testMtdId, testNino, testUtr)(OK)

        val res = IncomeTaxSubscriptionFrontend.handOffVAndC()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.getVAndCUrl)
        )
      }

      "session data is missing" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map())

        val res = IncomeTaxSubscriptionFrontend.handOffVAndC()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.getVAndCUrl)
        )
      }
    }
  }
}
