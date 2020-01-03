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

package controllers.agent.business

import java.time.LocalDate

import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels.{keystoreData, _}
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import _root_.agent.helpers.{ComponentSpecBase, IntegrationTestModels}
import _root_.agent.services.CacheConstants
import agent.models._
import agent.services.CacheConstants.IncomeSource
import core.config.featureswitch.FeatureSwitching
import core.models.{DateModel, No, Yes}
import incometax.business.models.AccountingPeriodModel
import incometax.subscription.models._
import incometax.util.AccountingPeriodUtil
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.libs.json.Json

class BusinessAccountingPeriodDateControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /business/accounting-period-dates" when {

    "keystore returns all data" should {
      "show accounting period dates page with date values entered" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.accounting_period.title")),
          mainHeading(Messages("agent.accounting_period.heading")),
          dateField("startDate", testAccountingPeriod.startDate),
          dateField("endDate", testAccountingPeriod.endDate)
        )
      }
    }

    "keystore returns no data" should {
      "show accounting period dates page without date values entered" in {
        val keystoreIncomeSource = Both
        val keystoreIncomeOther = No

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource),
            otherIncome = Some(keystoreIncomeOther)
          )
        )

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.accounting_period.title")),
          mainHeading(Messages("agent.accounting_period.heading")),
          dateField("startDate", DateModel("", "", "")),
          dateField("endDate", DateModel("", "", ""))
        )
      }
    }

  }


  "POST /business/accounting-period-dates" when {
    "ineligible dates" should {
      "redirect to Kickout page when the date range is incorrect" in {
        val userInput: AccountingPeriodModel = AccountingPeriodModel(testStartDate, testEndDate.plusDays(10))


        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(fullKeystoreData)
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot-use-service-yet")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(ineligibleURI)
        )
      }

      "redirect to the kickout page when a user with property income sources attempts to submit for the following tax year" in {
          val userInput: AccountingPeriodModel = AccountingPeriodModel(testStartDate.plusYears(1), testEndDate.plusYears(1))


          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(fullKeystoreData)
          KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

          When("POST /business/accounting-period-dates is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of cannot-use-service-yet")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(ineligibleURI)
          )
      }
    }
    "not in edit mode" should {

      "enter accounting period start and end dates on the accounting period page" in {
        val userInput: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(fullKeystoreData.updated(IncomeSource, Json.toJson(Business)))
        KeystoreStub.stubKeystoreSave(CacheConstants.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of business accounting method")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }

      "enter no accounting period dates on the accounting period page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
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
        "The new accounting period ends in the same tax year" in {
          val keystoreIncomeSource = Business
          val keystoreIncomeOther = No
          val startCurrenttestYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now().plusYears(-1))
          val endCurrenttestYear = startCurrenttestYear + 1
          val keystoreAccountingPeriodDates = AccountingPeriodModel(DateModel("06", "04", startCurrenttestYear.toString), DateModel("04", "04", endCurrenttestYear.toString))
          val userInput: AccountingPeriodModel = AccountingPeriodModel(DateModel("06", "04", startCurrenttestYear.toString), DateModel("05", "04", endCurrenttestYear.toString))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(
            keystoreData(
              incomeSource = Some(keystoreIncomeSource),
              otherIncome = Some(keystoreIncomeOther),
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
          val keystoreIncomeSource = Business
          val keystoreIncomeOther = No
          val startCurrenttestYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now().plusYears(-1))
          val endCurrenttestYear = startCurrenttestYear + 1
          val keystoreAccountingPeriodDates = AccountingPeriodModel(DateModel("06", "04", startCurrenttestYear.toString), DateModel("05", "04", endCurrenttestYear.toString))
          val userInput: AccountingPeriodModel = AccountingPeriodModel(DateModel("07", "04", startCurrenttestYear.toString), DateModel("06", "04", endCurrenttestYear.toString))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(
            keystoreData(
              incomeSource = Some(keystoreIncomeSource),
              otherIncome = Some(keystoreIncomeOther),
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
            redirectURI(termsURI)
          )
        }
      }

    }
  }
}
