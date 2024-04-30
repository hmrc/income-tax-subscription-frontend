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

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.{No, Yes}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utilities.SubscriptionDataKeys

class ClientCanSignUpControllerISpec extends ComponentSpecBase {

  "GET /client/can-sign-up" should {

    "return a status of OK" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /client/can-sign-up is called")
      val result: WSResponse = IncomeTaxSubscriptionFrontend.showCanSignUp

      Then("Should return a OK")
      result must have(
        httpStatus(OK),
        httpContentType(HTML)
      )
    }

  }

  "POST /client/can-sign-up" when {
    "the user selects continue signing up" should {
      s"return a redirect to ${controllers.agent.routes.WhatYouNeedToDoController.show().url}" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](
          id = SubscriptionDataKeys.EligibilityInterruptPassed,
          body = true
        )

        When("POST /client/can-sign-up is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCanSignUp(Some(Yes))

        Then("Should return SEE_OTHER to the home controller")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        )
      }
    }
    "the user selects check another client" should {
      s"return a redirect to ${controllers.agent.routes.AddAnotherClientController.addAnother().url}" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/can-sign-up is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCanSignUp(Some(No))

        Then("Should return SEE_OTHER to the add another client route")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.AddAnotherClientController.addAnother().url)
        )
      }
    }
    "the user selects no option" should {
      "return a status of BAD_REQUEST" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /client/can-sign-up is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCanSignUp(None)

        Then("Should return a BAD_REQUEST")
        result must have(
          httpStatus(BAD_REQUEST),
          httpContentType(HTML),
          errorDisplayed()
        )
      }
    }

  }

}
