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

package controllers.individual.incomesource

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status.OK

class CannotUseServiceControllerISpec extends ComponentSpecBase {


  "GET /report-quarterly/income-and-expenses/sign-up/error/cannot-use-service" should {
    "show the cannot sign up page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/cannot-use-service is called")
      val res = IncomeTaxSubscriptionFrontend.cannotUseService()
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      Then("Should return a OK with the cannot use service page")
      res should have(
        httpStatus(OK),
        pageTitle(messages("cannot-use-service.title") + serviceNameGovUk)
      )
    }
  }


}
