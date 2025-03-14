/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.individual.tasklist

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}

class IncomeSourcesIncompleteControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = "Use software to send Income Tax updates - GOV.UK"

  s"GET ${controllers.individual.tasklist.routes.IncomeSourcesIncompleteController.show.url}" when {
    "the user is not authorised" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.showIncomeSourcesIncomplete()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/details/income-sources-incomplete"))
        )
      }
    }
    "the user does not have any state" should {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showIncomeSourcesIncomplete(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.matching.routes.HomeController.index.url)
        )
      }
    }
    "the user is authorised and in a sign up state" should {
      "return OK with the page content" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showIncomeSourcesIncomplete()

        result must have(
          httpStatus(OK),
          pageTitle(s"${messages("individual.income-sources-incomplete.heading")} - $serviceNameGovUk")
        )
      }
    }
  }

  s"POST ${controllers.individual.tasklist.routes.IncomeSourcesIncompleteController.submit.url}" when {
    "the user is not authorised" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.submitIncomeSourcesIncomplete()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/details/income-sources-incomplete"))
        )
      }
    }
    "the user does not have any state" should {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitIncomeSourcesIncomplete()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.matching.routes.HomeController.index.url)
        )
      }
    }
    "the user is authorised and in a sign up state" should {
      "redirect the user to the your income sources page" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitIncomeSourcesIncomplete()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        )
      }
    }
  }

}
