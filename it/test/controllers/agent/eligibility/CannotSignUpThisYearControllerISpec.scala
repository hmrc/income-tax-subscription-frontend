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

package controllers.agent.eligibility

import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.{No, Yes}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class CannotSignUpThisYearControllerISpec extends ComponentSpecBase {

  "GET /client/error/cannot-sign-up-for-current-year" should {

    "return a status of OK" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /client/error/cannot-sign-up-for-current-year is called")
      val result: WSResponse = IncomeTaxSubscriptionFrontend.showCannotSignUpThisYear

      Then("Should return a OK")
      result.status mustBe OK
    }

  }

  "POST /client/error/cannot-sign-up-for-current-year" when {

    "the user selects yes" should {
      s"return a redirect to ${controllers.agent.routes.WhatYouNeedToDoController.show().url}" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/error/cannot-sign-up-for-current-year is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCannotSignUpThisYear(Some(Yes))

        Then("Should return SEE_OTHER to the home controller")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        )
      }
    }
    "the user selects no" should {
      s"return a redirect to ${controllers.agent.routes.DeclinedSignUpNextYearController.show.url}" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/error/cannot-sign-up-for-current-year is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCannotSignUpThisYear(Some(No))

        Then("Should return SEE_OTHER to the home controller")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.DeclinedSignUpNextYearController.show.url)
        )
      }
    }
    "the user selects no option" should {
      "return a status of BAD_REQUEST" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/error/cannot-sign-up-for-current-year is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCannotSignUpThisYear(None)

        Then("Should return a BAD_REQUEST")
        result.status mustBe BAD_REQUEST
      }
    }

  }

}
