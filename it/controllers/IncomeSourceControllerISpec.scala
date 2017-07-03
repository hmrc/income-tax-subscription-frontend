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
package controllers

import helpers.ComponentSpecBase
import helpers.servicemocks.{AuthStub, KeystoreStub}
import play.api.http.Status.OK
import play.api.i18n.Messages


class IncomeSourceControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/income" when {

    "keystore returns all data" should {
      "show the income source page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.income()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("income_source.title"))
          //todo check selected option
        )
      }
    }
  }

  "keystore returns no data" should {
    "show the income source page without an option selected" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubEmptyKeystore()

      When("GET /income is called")
      val res = IncomeTaxSubscriptionFrontend.income()

      Then("Should return a OK with the income source page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("income_source.title"))
        //todo check selected option
      )
    }
  }

}


