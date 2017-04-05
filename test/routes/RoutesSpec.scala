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
      controllers.routes.SessionTimeoutController.timeout().url must be (s"$contextRoute/session-timeout")
    }
  }

  "The URL for the SummaryController.showSummary action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      controllers.routes.CheckYourAnswersController.show().url must be (s"$contextRoute/check-your-answers")
    }
  }


  // Summary routes
  "The URL for the SummaryController.submitSummary action" should {
    s"be equal to $contextRoute/check-your-answers" in {
      controllers.routes.CheckYourAnswersController.submit().url must be (s"$contextRoute/check-your-answers")
    }
  }

  "The URL for the ConfirmationController.showConfirmation action" should {
    s"be equal to $contextRoute/confirmation" in {
      controllers.routes.ConfirmationController.showConfirmation().url must be (s"$contextRoute/confirmation")
    }
  }

  // Business accounting period prior to April 2017 routes
  "The URL for the BusinessAccountingPeriodController.show action" should {
    s"be equal to $contextRoute/business/accounting-period" in {
      controllers.business.routes.BusinessAccountingPeriodPriorController.show().url must be (s"$contextRoute/business/accounting-period-prior")
    }
  }

  "The URL for the BusinessAccountingPeriodController.submit action" should {
    s"be equal to $contextRoute/business/accounting-period" in {
      controllers.business.routes.BusinessAccountingPeriodPriorController.show().url must be (s"$contextRoute/business/accounting-period-prior")
    }
  }

  // Register Next Accounting Period routes
  "The URL for the RegisterNextAccountingPeriod.show action" should {
    s"be equal to $contextRoute/business/register-next-accounting-period" in {
      controllers.business.routes.RegisterNextAccountingPeriodController.show().url must be (s"$contextRoute/business/register-next-accounting-period")
    }
  }

  "The URL for the RegisterNextAccountingPeriod.submit action" should {
    s"be equal to $contextRoute/business/register-next-accounting-period" in {
      controllers.business.routes.RegisterNextAccountingPeriodController.show().url must be (s"$contextRoute/business/register-next-accounting-period")
    }
  }

  // Terms and Conditions routes
  "The URL for the Terms.showTerms(true) action" should {
    s"be equal to $contextRoute/terms?editMode=true" in {
      controllers.routes.TermsController.showTerms(true).url must be (s"$contextRoute/terms?editMode=true")
    }
  }

  "The URL for the Terms.submitTerms(true) action" should {
    s"be equal to $contextRoute/terms?editMode=true" in {
      controllers.routes.TermsController.submitTerms(true).url must be (s"$contextRoute/terms?editMode=true")
    }
  }

}
