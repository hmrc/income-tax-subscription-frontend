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
import helpers.IntegrationTestConstants.testNino
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status.OK
import play.api.libs.json.JsString

class NoSoftwareControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

  s"GET ${controllers.agent.routes.NoSoftwareController.show()}" should {
    "return OK and show the No Software page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      When(s"GET ${controllers.agent.routes.NoSoftwareController.show()}")
      val result = IncomeTaxSubscriptionFrontend.showNoSoftware()
      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("agent.no-software.heading") + serviceNameGovUk)
      )
    }
  }
}