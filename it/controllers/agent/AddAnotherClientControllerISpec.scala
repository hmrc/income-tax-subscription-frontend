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

import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import _root_.agent.helpers.{ComponentSpecBase, SessionCookieCrumbler}
import core.config.featureswitch.{EligibilityPagesFeature, FeatureSwitching}
import play.api.http.Status.SEE_OTHER


class AddAnotherClientControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
  }

  "GET /add-another" when {

    "the eligibility pages feature switch is enabled" should {
      s"clear the keystore and ${ITSASessionKeys.MTDITID} & ${ITSASessionKeys.JourneyStateKey} session variables" in {
        Given("I setup the wiremock stubs and feature switch is enabled")
        enable(EligibilityPagesFeature)
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreDelete()

        When("I call GET /add-another")
        val res = IncomeTaxSubscriptionFrontend.getAddAnotherClient(hasSubmitted = true)
        val expectedRedirect: String = s"$mockUrl/report-quarterly/income-and-expenses/sign-up/eligibility/client/other-income"

        Then(s"The result should have a status of SEE_OTHER and redirect to '$expectedRedirect'")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(expectedRedirect)
        )

        val cookie = SessionCookieCrumbler.getSessionMap(res)
        cookie.keys should not contain ITSASessionKeys.MTDITID
        cookie.keys should not contain ITSASessionKeys.JourneyStateKey
        cookie.keys should not contain ITSASessionKeys.UnauthorisedAgentKey

        KeystoreStub.verifyKeyStoreDelete(Some(1))
      }
    }

    "the eligibility pages feature switch is disabled" should {
      s"clear the keystore and ${ITSASessionKeys.MTDITID} & ${ITSASessionKeys.JourneyStateKey} session variables" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreDelete()

        When("I call GET /add-another")
        val res = IncomeTaxSubscriptionFrontend.getAddAnotherClient(hasSubmitted = true)

        Then(s"The result should have a status of SEE_OTHER and redirect to ${controllers.agent.matching.routes.ClientDetailsController.show().url}")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.ClientDetailsController.show().url)
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
