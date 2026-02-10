/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.individual.handoffs

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}

class CheckIncomeSourcesControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.CheckIncomeSourcesController.show.url}" must {
    "display the check income sources page" when {
      "the user has the MTDITID enrolment" in {
        AuthStub.stubEnrolled()

        val result = IncomeTaxSubscriptionFrontend.getCheckIncomeSources

        result must have(
          httpStatus(OK),
          pageTitle(messages("individual.check-income-sources.heading") + serviceNameGovUk)
        )
      }
      "the user does not have the MTDITID enrolment" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.getCheckIncomeSources

        result must have(
          httpStatus(OK),
          pageTitle(messages("individual.check-income-sources.heading") + serviceNameGovUk)
        )
      }
    }
  }

  s"POST ${routes.CheckIncomeSourcesController.submit.url}" must {
    "redirect to the view and change service home page" when {
      "the user has the MTDITID enrolment" in {
        AuthStub.stubEnrolled()

        val result = IncomeTaxSubscriptionFrontend.postCheckIncomeSources

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.getVAndCUrl)
        )
      }
    }
    "redirect to the sign out route with a continue url of the view and change home page" when {
      "the user does not have the MTDITID enrolment" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.postCheckIncomeSources

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.ggSignOutUrl(appConfig.getVAndCUrl))
        )
      }
    }
  }

}
