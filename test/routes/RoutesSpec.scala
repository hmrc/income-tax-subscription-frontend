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

  val contextRoute: String = "/income-tax-subscription-frontend"

  "The URL for the timeout.timeout action" should {
    s"be equal to $contextRoute/session-timeout" in {
      controllers.routes.SessionTimeoutController.timeout().url must be (s"$contextRoute/session-timeout")
    }
  }

  "The URL for the SummaryController.showSummary action" should {
    s"be equal to $contextRoute/summary" in {
      controllers.routes.SummaryController.showSummary().url must be (s"$contextRoute/summary")
    }
  }

  "The URL for the SummaryController.submitSummary action" should {
    s"be equal to $contextRoute/summary" in {
      controllers.routes.SummaryController.submitSummary().url must be (s"$contextRoute/summary")
    }
  }

  "The URL for the ConfirmationController.showConfirmation action" should {
    s"be equal to $contextRoute/confirmation" in {
      controllers.routes.ConfirmationController.showConfirmation().url must be (s"$contextRoute/confirmation")
    }
  }

}
