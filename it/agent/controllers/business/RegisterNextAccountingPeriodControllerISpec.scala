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

package agent.controllers.business

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class RegisterNextAccountingPeriodControllerISpec extends ComponentSpecBase {

  "GET /business/register-next-accounting-period" when {

    "keystore not applicable" should {
      "show the register next accounting period page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /error/register-next-accounting-period is called")
        val res = IncomeTaxSubscriptionFrontend.registerNextAccountingPeriod()

        Then("Should return a OK with the error main income page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.business.register_next_accounting_period.title"))
        )
      }
    }

  }


  "POST /business/register-next-accounting-period" when {

    "always" should {

      "select the continue to sign up button" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /business/register-next-accounting-period is called")
        val res = IncomeTaxSubscriptionFrontend.submitRegisterNextAccountingPeriod()

        Then("Should return a SEE_OTHER with a redirect location of accounting period dates page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingPeriodDatesURI)
        )
      }

    }
  }
}
