/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels._
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import core.models.Yes
import incometax.subscription.models.{Both, Business, Property}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class OtherIncomeErrorControllerISpec extends ComponentSpecBase {

  "GET /error/other-income" should {
    "show the error other income page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/other-income is called")
      val res = IncomeTaxSubscriptionFrontend.otherIncomeError()

      Then("Should return a OK with the error other income page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("agent.other-income-error.title"))
      )
    }
  }


  "POST /error/other-income" when {

    "not in edit mode" should {

      "select the Continue button on the error other income page whilst on Business journey" in {
        val keystoreIncomeSource = Business
        val keystoreIncomeOther = Yes

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther)
          )
        )

        When("POST /error/other-income is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncomeError()

        Then("Should return a SEE_OTHER with a redirect location of accounting period prior")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingPeriodPriorURI)
        )
      }

      "select the Continue button on the error other income page whilst on Both journey" in {
        val keystoreIncomeSource = Both
        val keystoreIncomeOther = Yes

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther)
          )
        )

        When("POST /error/other-income is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncomeError()

        Then("Should return a SEE_OTHER with a redirect location of accounting period prior")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingPeriodPriorURI)
        )
      }

      "select the Continue button on the error other income page whilst on Property journey" in {
        val keystoreIncomeSource = Property
        val keystoreIncomeOther = Yes

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther)
          )
        )

        When("POST /error/other-income is called")
        val res = IncomeTaxSubscriptionFrontend.submitOtherIncomeError()

        Then("Should return a SEE_OTHER with a redirect location of terms page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(termsURI)
        )
      }
    }
  }
}
