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

package incometax.subscription

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status.OK
import play.api.i18n.Messages

class ExitSurveyThankYouControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/feedback-submitted" should {
    "show the feedback submitted page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /feedback-submitted is called")
      val res = IncomeTaxSubscriptionFrontend.exitSurveyThankYou()

      Then("Should return a OK with the feedback submitted page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("thank-you.title"))
      )
    }
  }

}
