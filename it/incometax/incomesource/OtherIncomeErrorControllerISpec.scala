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

package incometax.incomesource

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class OtherIncomeErrorControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/error/other-income" when {

    "keystore not applicable" should {
      "show the error other income page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /error/other-income is called")
        val res = IncomeTaxSubscriptionFrontend.otherIncomeError()

        Then("Should return a OK with the error other income page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("other-income-error.title"))
        )
      }
    }
  }


  "POST /report-quarterly/income-and-expenses/sign-up/error/other-income" when {

    "not in edit mode" should {

      "select the Continue button on the error other income page whilst on Business journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_business)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_yes)

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

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "select the Continue button on the error other income page whilst on Both journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_yes)

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

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "select the Continue button on the error other income page whilst on Property journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_property)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_yes)

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
