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

package agent.controllers

import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import _root_.agent.helpers.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status.{NOT_FOUND, SEE_OTHER}


class AddAnotherClientControllerISpec extends ComponentSpecBase {

  "GET /add-another" when {

    "the session marked the journey as complete" should {
      s"clear the keystore and ${ITSASessionKeys.MTDITID} & ${ITSASessionKeys.JourneyStateKey} session variables" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreDelete()

        When("I call GET /add-another")
        val res = IncomeTaxSubscriptionFrontend.getAddAnotherClient(hasSubmitted = true)

        Then(s"The result should have a status of SEE_OTHER and redirect to ${agent.controllers.matching.routes.ClientDetailsController.show().url}")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(agent.controllers.matching.routes.ClientDetailsController.show().url)
        )

        val cookie = SessionCookieCrumbler.getSessionMap(res)
        cookie.keys should not contain ITSASessionKeys.MTDITID
        cookie.keys should not contain ITSASessionKeys.JourneyStateKey
        cookie.keys should not contain ITSASessionKeys.UnauthorisedAgentKey

        KeystoreStub.verifyKeyStoreDelete(Some(1))
      }
    }

  }

}
