/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.individual.eligibility

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class CannotSignUpThisYearControllerISpec extends ComponentSpecBase {

  "GET /error/cannot-sign-up-for-current-year" should {

    "return a status of OK" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/cannot-sign-up-for-current-year is called")
      val result: WSResponse = IncomeTaxSubscriptionFrontend.showCannotSignUpThisYear

      Then("Should return a OK")
      result.status mustBe OK
    }

  }

  "POST /error/cannot-sign-up-for-current-year" should {

    s"return a redirect to ${controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url}" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("POST /error/cannot-sign-up-for-current-year is called")
      val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCannotSignUpThisYear

      Then("Should return SEE_OTHER to the sps handoff")

      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url)
      )
    }

  }

}
