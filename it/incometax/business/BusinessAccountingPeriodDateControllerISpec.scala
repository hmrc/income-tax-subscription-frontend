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

package incometax.business

import java.time.LocalDate

import core.config.featureswitch.FeatureSwitching
import core.models.{DateModel, No}
import core.services.CacheConstants
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import helpers.{ComponentSpecBase, IntegrationTestModels}
import incometax.business.models.AccountingPeriodModel
import play.api.http.Status._
import play.api.i18n.Messages

class BusinessAccountingPeriodDateControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /report-quarterly/income-and-expenses/sign-up/business/accounting-period-dates" when {

    "keystore returns all data" should {
      "show the accounting period dates page with date values entered" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("accounting_period.title")),
          mainHeading(Messages("accounting_period.heading.signup")),
          dateField("startDate", testAccountingPeriod.startDate),
          dateField("endDate", testAccountingPeriod.endDate)
        )
      }
    }

    "keystore returns no data" should {
      "show the accounting period dates page without date values entered" in {
        val keystoreIncomeOther = No
        val keystoreMatchTaxYear = testMatchTaxYearNo

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            rentUkProperty = Some(testRentUkProperty_property_and_other),
            workForYourself = Some(testWorkForYourself_yes),
            otherIncome = Some(keystoreIncomeOther),
            matchTaxYear = Some(keystoreMatchTaxYear)
          )
        )

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("accounting_period.title")),
          mainHeading(Messages("accounting_period.heading.signup")),
          dateField("startDate", DateModel("", "", "")),
          dateField("endDate", DateModel("", "", ""))
        )
      }
    }

  }


  "POST /report-quarterly/income-and-expenses/sign-up/business/accounting-period-dates" when {
    val keystoreMatchTaxYear = testMatchTaxYearNo

    "not in edit mode" when {

      "enter accounting period start and end dates on the accounting period page" in {

        val userInput: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            rentUkProperty = Some(testRentUkProperty_no_property),
            workForYourself = Some(testWorkForYourself_yes),
            matchTaxYear = Some(keystoreMatchTaxYear)))

        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of accounting method")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }


      "enter accounting period after the 2017 - 2018 tax year on the accounting period page" in {

        val userInput: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod(testStartDate, testEndDateNext)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            rentUkProperty = Some(testRentUkProperty_no_property),
            workForYourself = Some(testWorkForYourself_yes),
            matchTaxYear = Some(keystoreMatchTaxYear))
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
          keystoreData(matchTaxYear = Some(testMatchTaxYearNo))
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
          keystoreData(matchTaxYear = Some(testMatchTaxYearNo))
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

      "simulate changing accounting period dates when calling page from Check Your Answers" when {
        "the new accounting period ends in the same tax year" in {
          val keystoreIncomeOther = No
          val keystoreMatchTaxYear = testMatchTaxYearNo
          val startCurrenttestYear = LocalDate.now.plusYears(1).getYear
          val endCurrenttestYear = startCurrenttestYear + 1
          val keystoreAccountingPeriodDates = AccountingPeriodModel(DateModel("06", "05", startCurrenttestYear.toString), DateModel("04", "05", endCurrenttestYear.toString))
          val userInput: AccountingPeriodModel = AccountingPeriodModel(DateModel("06", "05", startCurrenttestYear.toString), DateModel("05", "05", endCurrenttestYear.toString))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(
            keystoreData(
              rentUkProperty = Some(testRentUkProperty_property_and_other),
              workForYourself = Some(testWorkForYourself_yes),
              otherIncome = Some(keystoreIncomeOther),
              matchTaxYear = Some(keystoreMatchTaxYear),
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

        "The new accounting period ends in a different tax year" in {
          val keystoreIncomeOther = No
          val keystoreMatchTaxYear = testMatchTaxYearNo
          val startCurrenttestYear = LocalDate.now.plusYears(1).getYear
          val endCurrenttestYear = startCurrenttestYear + 1
          val keystoreAccountingPeriodDates = AccountingPeriodModel(DateModel("07", "05", startCurrenttestYear.toString), DateModel("06", "05", (endCurrenttestYear + 1).toString))
          val userInput: AccountingPeriodModel = AccountingPeriodModel(DateModel("07", "05", startCurrenttestYear.toString), DateModel("06", "05", endCurrenttestYear.toString))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(
            keystoreData(
              rentUkProperty = Some(testRentUkProperty_no_property),
              workForYourself = Some(testWorkForYourself_yes),
              otherIncome = Some(keystoreIncomeOther),
              matchTaxYear = Some(keystoreMatchTaxYear),
              accountingPeriodDate = Some(keystoreAccountingPeriodDates)
            )
          )
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)
          KeystoreStub.stubKeystoreSave(CacheConstants.Terms, false)

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

}
