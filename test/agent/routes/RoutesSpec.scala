/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest

class RoutesSpec extends PlaySpec with GuiceOneAppPerTest {

  val contextRoute: String = "/report-quarterly/income-and-expenses/sign-up/client"

  // Timeout routes
  "The URL for the timeout.show action" should {
    s"be equal to $contextRoute/session-timeout" in {
      controllers.agent.routes.SessionTimeoutController.show.url must be(s"$contextRoute/session-timeout")
    }
  }

  "The URL for the SummaryController.show action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      controllers.agent.routes.CheckYourAnswersController.show.url must be(s"$contextRoute/check-your-answers")
    }
  }


  // Summary routes
  "The URL for the SummaryController.submit action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      controllers.agent.routes.CheckYourAnswersController.submit.url must be(s"$contextRoute/check-your-answers")
    }
  }

  "The URL for the ConfirmationController.show action" should {
    s"be equal to $contextRoute/confirmation" in {
      controllers.agent.routes.ConfirmationAgentController.show.url must be(s"$contextRoute/confirmation")
    }
  }

}
