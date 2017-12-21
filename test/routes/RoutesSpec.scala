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

package routes

import org.scalatestplus.play.{OneAppPerTest, PlaySpec}

class RoutesSpec extends PlaySpec with OneAppPerTest {

  val contextRoute: String = "/report-quarterly/income-and-expenses/sign-up"

  // Timeout routes
  "The URL for the timeout.timeout action" should {
    s"be equal to $contextRoute/session-timeout" in {
      core.controllers.routes.SessionTimeoutController.timeout().url must be(s"$contextRoute/session-timeout")
    }
  }

  "The URL for the SummaryController.showSummary action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      incometax.subscription.controllers.routes.CheckYourAnswersController.show().url must be(s"$contextRoute/check-your-answers")
    }
  }


  // Summary routes
  "The URL for the SummaryController.submitSummary action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      incometax.subscription.controllers.routes.CheckYourAnswersController.submit().url must be(s"$contextRoute/check-your-answers")
    }
  }

  "The URL for the ConfirmationController.showConfirmation action" should {
    s"be equal to $contextRoute/confirmation" in {
      incometax.subscription.controllers.routes.ConfirmationController.showConfirmation().url must be(s"$contextRoute/confirmation")
    }
  }

  // Terms and Conditions routes
  "The URL for the Terms.showTerms action" should {
    s"be equal to $contextRoute/terms" in {
      incometax.subscription.controllers.routes.TermsController.showTerms().url must be(s"$contextRoute/terms")
    }
  }

  "The URL for the Terms.submitTerms(true) action" should {
    s"be equal to $contextRoute/terms" in {
      incometax.subscription.controllers.routes.TermsController.submitTerms().url must be(s"$contextRoute/terms")
    }
  }

}
