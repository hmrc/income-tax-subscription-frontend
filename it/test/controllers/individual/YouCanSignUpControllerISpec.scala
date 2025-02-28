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

package controllers.individual

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.servicemocks.AuthStub
import play.api.test.Helpers._

class YouCanSignUpControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  s"GET ${routes.YouCanSignUpController.show.url}" when {
    "the user is authenticated" must {
      "return OK with the page content" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.youCanSignUp()

        result must have(
          httpStatus(OK),
          pageTitle(messages("individual.you-can-sign-up-now.heading") + serviceNameGovUk)
        )
      }
      "the user is unauthenticated" must {
        "redirect to the login page" in {
          AuthStub.stubUnauthorised()

          val result = IncomeTaxSubscriptionFrontend.youCanSignUp()

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(basGatewaySignIn("/you-can-sign-up-now"))
          )
        }
      }
    }
  }
}