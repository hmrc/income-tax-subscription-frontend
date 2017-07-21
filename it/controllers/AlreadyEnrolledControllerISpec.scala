/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class AlreadyEnrolledControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/error/subscription-status" when {

    "keystore not applicable" should {
      "show the already enrolled page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubEnrolled()

        When("GET /error/subscription-status is called")
        val res = IncomeTaxSubscriptionFrontend.alreadyEnrolled()

        Then("Should return a OK with the already enrolled page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("already-enrolled.title"))
        )
      }
    }
  }
}
