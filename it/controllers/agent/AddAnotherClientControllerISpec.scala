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

import config.featureswitch.FeatureSwitch.RemoveCovidPages
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status.SEE_OTHER


class AddAnotherClientControllerISpec extends ComponentSpecBase with SessionCookieCrumbler with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(RemoveCovidPages)
    super.beforeEach()
  }

  "GET /add-another - when RemoveCovidPages FS Enabled" when {
    s"clear the Subscription Details  and ${ITSASessionKeys.MTDITID} & ${ITSASessionKeys.JourneyStateKey} session variables" in {
      Given("I setup the wiremock stubs and feature switch is enabled")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionDeleteAll()

      When("I call GET /add-another")
      enable(RemoveCovidPages)
      val res = IncomeTaxSubscriptionFrontend.getAddAnotherClient(hasSubmitted = true)
      val expectedRedirect: String = eligibility.routes.OtherSourcesOfIncomeController.show().url

      Then(s"The result should have a status of SEE_OTHER and redirect to '$expectedRedirect'")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(expectedRedirect)
      )

      val cookie = getSessionMap(res)
      cookie.keys should not contain ITSASessionKeys.MTDITID
      cookie.keys should not contain ITSASessionKeys.JourneyStateKey

      IncomeTaxSubscriptionConnectorStub.verifySubscriptionDelete(Some(1))
    }
  }

  "GET /add-another" when {
    s"clear the Subscription Details  and ${ITSASessionKeys.MTDITID} & ${ITSASessionKeys.JourneyStateKey} session variables" in {
      Given("I setup the wiremock stubs and feature switch is enabled")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionDeleteAll()

      When("I call GET /add-another")
      val res = IncomeTaxSubscriptionFrontend.getAddAnotherClient(hasSubmitted = true)
      val expectedRedirect: String = eligibility.routes.Covid19ClaimCheckController.show().url

      Then(s"The result should have a status of SEE_OTHER and redirect to '$expectedRedirect'")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(expectedRedirect)
      )

      val cookie = getSessionMap(res)
      cookie.keys should not contain ITSASessionKeys.MTDITID
      cookie.keys should not contain ITSASessionKeys.JourneyStateKey

      IncomeTaxSubscriptionConnectorStub.verifySubscriptionDelete(Some(1))
    }
  }

}
