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

package controllers.agent.matching

import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub


class CannotGoBackToPreviousClientControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/cannot-go-back-to-previous-client" should {
    "show the Cannot Go Back To Previous Client page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /client/business/what-year-to-sign-up is called")
      val res = IncomeTaxSubscriptionFrontend.showCannotGoBackToPreviousClient()


      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      Then("Should return a OK with the Cannot Go Back To Previous Client page")
      res must have(
        httpStatus(200),
        pageTitle(messages("agent.cannot-go-back-previous-client.title") + serviceNameGovUk),
        radioButtonSet(id = "cannotGoBackToPreviousClient", selectedRadioButton = None),
        radioButtonSet(id = "cannotGoBackToPreviousClient-2", selectedRadioButton = None),
        radioButtonSet(id = "cannotGoBackToPreviousClient-3", selectedRadioButton = None)
      )
    }
  }

}


