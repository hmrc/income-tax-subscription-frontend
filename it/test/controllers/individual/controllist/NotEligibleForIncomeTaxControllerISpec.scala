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

package controllers.individual.controllist

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.testNino
import helpers.servicemocks.AuthStub
import models.EligibilityStatus
import play.api.http.Status.OK
import play.api.libs.json.{JsString, Json}

class NotEligibleForIncomeTaxControllerISpec extends ComponentSpecBase {


  "GET /report-quarterly/income-and-expenses/sign-up/cannot-use-service-yet" should {
    "show the cannot use service yet page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetAllSessionData(Map(
        ITSASessionKeys.NINO -> JsString(testNino),
        ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(false,false,None))
      ))

      When("GET /cannot-use-service-yet")
      val res = IncomeTaxSubscriptionFrontend.notEligibleForIncomeTax()
      val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
      Then("Should return a OK with the cannot use service yet page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("You cannot sign up yet") + serviceNameGovUk)
      )
    }
  }


}
