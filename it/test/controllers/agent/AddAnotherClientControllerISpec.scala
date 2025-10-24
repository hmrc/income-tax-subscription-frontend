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

package controllers.agent

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.JsBoolean

class AddAnotherClientControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  s"GET ${routes.AddAnotherClientController.addAnother().url}" should {
    "redirect to the login page" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.getAddAnotherClient

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/add-another"))
        )
      }
    }
    "redirect to the enter client details page" when {
      "email passed is present in session" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.EMAIL_PASSED -> JsBoolean(true)
        ))
        SessionDataConnectorStub.stubDeleteAllSessionData(OK)
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.EMAIL_PASSED, true)(OK)

        val result = IncomeTaxSubscriptionFrontend.getAddAnotherClient

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.ClientDetailsController.show().url)
        )
      }
    }
    "return an internal server error" when {
      "there was a problem deleting session data" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.EMAIL_PASSED -> JsBoolean(true)
        ))
        SessionDataConnectorStub.stubDeleteAllSessionData(INTERNAL_SERVER_ERROR)

        val result = IncomeTaxSubscriptionFrontend.getAddAnotherClient

        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "there was a problem when saving the email passed session flag" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.EMAIL_PASSED -> JsBoolean(true)
        ))
        SessionDataConnectorStub.stubDeleteAllSessionData(OK)
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.EMAIL_PASSED, true)(INTERNAL_SERVER_ERROR)

        val result = IncomeTaxSubscriptionFrontend.getAddAnotherClient

        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
