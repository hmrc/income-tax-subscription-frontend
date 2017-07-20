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

import forms.{AccountingMethodForm, AccountingPeriodPriorForm, IncomeSourceForm, OtherIncomeForm}
import helpers.{ComponentSpecBase, IntegrationTestModels}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import models._
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class TermsControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/terms" when {

    "keystore returns all data" should {
      "show the terms page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /terms is called")
        val res = IncomeTaxSubscriptionFrontend.terms()

        Then("Should return a OK with the other income page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("terms.title"))
        )
      }
    }

    "redirect to sign-in when auth fails" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("GET /terms is called")
      val res = IncomeTaxSubscriptionFrontend.terms()

      Then("Should return a SEE_OTHER with a redirect location of sign-in")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(signInURI)
      )
    }

  }


  "POST /report-quarterly/income-and-expenses/sign-up/terms" when {

    "not in edit mode" should {

      "select the Continue button on the terms page" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
        val keystoreAccountingPeriodDates: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod
        val keystoreAccountingMethod = AccountingMethodModel(AccountingMethodForm.option_cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates),
            accountingMethod = Some(keystoreAccountingMethod)
          )
        )

        When("POST /terms is called")
        val res = IncomeTaxSubscriptionFrontend.submitTerms()

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }
}
