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

package incometax.business

import core.models.DateModel
import core.services.CacheConstants
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import helpers.{ComponentSpecBase, IntegrationTestModels}
import incometax.business.forms.AccountingPeriodPriorForm
import incometax.business.models.{AccountingPeriodModel, AccountingPeriodPriorModel}
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}
import play.api.http.Status._
import play.api.i18n.Messages

class BusinessAccountingPeriodDateControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/accounting-period-dates" when {

    "keystore returns all data" should {
      "show the current accounting period dates page with date values entered" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("accounting_period.title")),
          mainHeading(Messages("accounting_period.heading.current")),
          dateField("startDate", testAccountingPeriod.startDate),
          dateField("endDate", testAccountingPeriod.endDate)
        )
      }
    }

    "keystore returns no data" should {
      "show the current accounting period dates page without date values entered" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior)
          )
        )

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("accounting_period.title")),
          mainHeading(Messages("accounting_period.heading.current")),
          dateField("startDate", DateModel("", "", "")),
          dateField("endDate", DateModel("", "", ""))
        )
      }
    }

    "keystore returns all data" should {
      "show the future accounting period dates page with date values entered" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)
        val keystoreAccountingPeriodDates: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates)
          )
        )

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("accounting_period.title")),
          mainHeading(Messages("accounting_period.heading.next")),
          dateField("startDate", testAccountingPeriod.startDate),
          dateField("endDate", testAccountingPeriod.endDate)
        )
      }
    }

    "keystore returns no data" should {
      "show the future accounting period dates page without date values entered" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior)
          )
        )

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("accounting_period.title")),
          mainHeading(Messages("accounting_period.heading.next")),
          dateField("startDate", DateModel("", "", "")),
          dateField("endDate", DateModel("", "", ""))
        )
      }
    }

  }


  "POST /report-quarterly/income-and-expenses/sign-up/business/accounting-period-dates" when {
    val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

    "not in edit mode" should {

      "enter accounting period start and end dates on the accounting period page" in {
        val userInput: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(accountingPeriodPrior = Some(keystoreAccountingPeriodPrior))
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of accounting method")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }

      "enter no accounting period dates on the accounting period page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(accountingPeriodPrior = Some(keystoreAccountingPeriodPrior))
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, "")

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select invalid income source option on the income source page as if the user it trying to manipulate the html" in {
        val userInput = AccountingPeriodModel(DateModel("dd", "mm", "yyyy"), DateModel("dd", "mm", "yyyy"))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(accountingPeriodPrior = Some(keystoreAccountingPeriodPrior))
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {

      "simulate not changing accounting period dates when calling page from Check Your Answers" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
        val keystoreAccountingPeriodDates = AccountingPeriodModel(DateModel("06", "04", "2017"), DateModel("05", "04", "2018"))
        val userInput: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing accounting period dates when calling page from Check Your Answers" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)
        val keystoreIncomeOther = OtherIncomeModel(OtherIncomeForm.option_no)
        val keystoreAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
        val keystoreAccountingPeriodDates = AccountingPeriodModel(DateModel("07", "05", "2018"), DateModel("06", "05", "2019"))
        val userInput: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther),
            accountingPeriodPrior = Some(keystoreAccountingPeriodPrior),
            accountingPeriodDate = Some(keystoreAccountingPeriodDates)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }
  }
}
