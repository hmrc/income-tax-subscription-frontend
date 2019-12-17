/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.routes

import org.scalatestplus.play.{OneAppPerTest, PlaySpec}

class RoutesSpec extends PlaySpec with OneAppPerTest {

  val contextRoute: String = "/report-quarterly/income-and-expenses/sign-up/client"

  // Timeout routes
  "The URL for the timeout.show action" should {
    s"be equal to $contextRoute/session-timeout" in {
      agent.controllers.routes.SessionTimeoutController.show().url must be (s"$contextRoute/session-timeout")
    }
  }

  "The URL for the SummaryController.show action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      agent.controllers.routes.CheckYourAnswersController.show().url must be (s"$contextRoute/check-your-answers")
    }
  }


  // Summary routes
  "The URL for the SummaryController.submit action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      agent.controllers.routes.CheckYourAnswersController.submit().url must be (s"$contextRoute/check-your-answers")
    }
  }

  "The URL for the ConfirmationController.show action" should {
    s"be equal to $contextRoute/confirmation" in {
      agent.controllers.routes.ConfirmationController.show().url must be (s"$contextRoute/confirmation")
    }
  }

  // Terms and Conditions routes
  "The URL for the Terms.show action" should {
    s"be equal to $contextRoute/terms" in {
      agent.controllers.routes.TermsController.show().url must be (s"$contextRoute/terms")
    }
  }

  "The URL for the Terms.submit() action" should {
    s"be equal to $contextRoute/terms" in {
      agent.controllers.routes.TermsController.submit().url must be (s"$contextRoute/terms")
    }
  }


  // Agent not authorised Confirmation routes
  "The URL for the unauthorised Sign up complete.show action" should {
    s"be equal to $contextRoute/send-client-link" in {
      agent.controllers.routes.UnauthorisedAgentConfirmationController.show().url must be (s"$contextRoute/send-client-link")
    }
  }

}
