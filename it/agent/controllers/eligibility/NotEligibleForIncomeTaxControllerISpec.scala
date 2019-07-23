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

package agent.controllers.eligibility

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status.OK
import play.api.i18n.Messages

class NotEligibleForIncomeTaxControllerISpec extends ComponentSpecBase {


  "GET  /report-quarterly/income-and-expenses/sign-up/client/cannot-use-service-yet" should {
    "show the agent cannot use service yet page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /cannot-use-service-yet")
      val res = IncomeTaxSubscriptionFrontend.notEligibleForIncomeTax()

      Then("Should return a OK with the agent cannot use service yet page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("agent_not_eligible_for_income_tax.title"))
      )
    }
  }


}
