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

package controllers.individual.accountingperiod

import config.featureswitch.FeatureSwitching
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class AccountingPeriodNotSupportedControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  s"GET ${controllers.individual.accountingperiod.routes.AccountingPeriodNotSupportedController.show.url}" when {
    "user is not authorised" must {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.accountingPeriodNotSupported

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/accounting-period-not-supported"))
        )
      }
    }
    "user is authorised" must {
      "return OK" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.accountingPeriodNotSupported

        result must have(
          httpStatus(OK),
          pageTitle(messages("accounting-period-not-supported.heading") + serviceNameGovUk),
        )
      }
    }
  }
}
