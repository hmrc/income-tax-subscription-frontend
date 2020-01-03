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

package controllers.individual.subscription

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class ExitSurveyControllerISpec extends ComponentSpecBase {
  "GET /exit-survey" when {
    "a user is or isn't logged in" should {
      "return the exit survey page" in {
        Given("No wiremock stubs are set up")

        val testOrigin = "/origin"

        When("I call GET /exit-survey")
        val res = IncomeTaxSubscriptionFrontend.exitSurvey(origin = testOrigin)

        Then("the result should have a status of OK and the exit survey page title")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("exit-survey.title"))
        )
      }
    }
  }

  "POST /exit-survey" when {

    "a user is or isn't logged in" should {

      "redirect to the feedback submitted page" in {
        Given("No wiremock stubs are set up")

        When("POST /exit-survey is called")
        val res = IncomeTaxSubscriptionFrontend.submitExitSurvey()

        Then("Should return a SEE_OTHER with a redirect location of feedback submitted page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(feedbackSubmittedURI)
        )
      }
    }
  }

}
