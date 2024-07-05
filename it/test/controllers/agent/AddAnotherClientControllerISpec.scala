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
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status.{OK, SEE_OTHER}
import services.{AgentEndOfJourneyThrottle, AgentStartOfJourneyThrottle}


class AddAnotherClientControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  "GET /add-another" when {
    s"clear the Subscription Details session variables" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubDeleteSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(OK)
      SessionDataConnectorStub.stubDeleteSessionData(ITSASessionKeys.throttlePassed(AgentEndOfJourneyThrottle))(OK)
      SessionDataConnectorStub.stubDeleteSessionData(ITSASessionKeys.MANDATION_STATUS)(OK)
      SessionDataConnectorStub.stubDeleteSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK)
      SessionDataConnectorStub.stubDeleteSessionData(ITSASessionKeys.NINO)(OK)
      SessionDataConnectorStub.stubDeleteSessionData(ITSASessionKeys.REFERENCE)(OK)
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionDeleteAll()

      When("I call GET /add-another")
      val res = IncomeTaxSubscriptionFrontend.getAddAnotherClient(hasSubmitted = true)
      val expectedRedirect: String = controllers.agent.matching.routes.ClientDetailsController.show().url

      Then(s"The result must have a status of SEE_OTHER and redirect to '$expectedRedirect'")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(expectedRedirect)
      )

      val cookie = getSessionMap(res)
      cookie.keys must not contain ITSASessionKeys.MTDITID
    }
  }

}
