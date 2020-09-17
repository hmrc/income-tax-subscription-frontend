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

import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels.{subscriptionData, _}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, IntegrationTestModels}
import models.DateModel
import models.individual.business.AccountingPeriodModel
import models.individual.subscription.{Both, Business}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

class BusinessAccountingPeriodDateControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /business/accounting-period-dates" when {

    "the Subscription Details Connector returns all data" should {
      "show accounting period dates page with date values entered" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.accounting_period.title")),
          mainHeading(messages("agent.accounting_period.heading")),
          dateField("startDate", testAccountingPeriod.startDate),
          dateField("endDate", testAccountingPeriod.endDate)
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show accounting period dates page without date values entered" in {
        val SubscriptionDetailsIncomeSource = Both

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            incomeSource = Some(SubscriptionDetailsIncomeSource)
          )
        )

        When("GET /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.businessAccountingPeriodDates()

        Then("Should return a OK with the accounting period dates page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.accounting_period.title")),
          mainHeading(messages("agent.accounting_period.heading")),
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
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(fullSubscriptionData)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot take part")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show().url)
        )
      }

      "redirect to the kickout page when a user with property income sources attempts to submit for the following tax year" in {
        val userInput: AccountingPeriodModel = AccountingPeriodModel(testStartDate.plusYears(1), testEndDate.plusYears(1))


        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(fullSubscriptionData)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingPeriodDate, userInput)

        When("POST /business/accounting-period-dates is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingPeriodDates(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot take part")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show().url)
        )
      }
    }
    "not in edit mode" should {

      "enter accounting period start and end dates on the accounting period page" in {
        val userInput: AccountingPeriodModel = IntegrationTestModels.testAccountingPeriod

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(fullSubscriptionData.updated(SubscriptionDataKeys.IncomeSource, Json.toJson(Business)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingPeriodDate, userInput)

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
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingPeriodDate, "")

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
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingPeriodDate, userInput)

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

      "simulate changing accounting period dates when calling page from Check Your Answers" in {
        val SubscriptionDetailsIncomeSource = Business
        val startCurrenttestYear = AccountingPeriodUtil.getTaxEndYear(LocalDate.now().plusYears(-1))
        val endCurrenttestYear = startCurrenttestYear + 1
        val SubscriptionDetailsAccountingPeriodDates = AccountingPeriodModel(DateModel("06", "04", startCurrenttestYear.toString),
                                                                                                    DateModel("04", "04", endCurrenttestYear.toString))
        val userInput: AccountingPeriodModel = AccountingPeriodModel(DateModel("06", "04", startCurrenttestYear.toString),
                                                                                                    DateModel("05", "04", endCurrenttestYear.toString))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            incomeSource = Some(SubscriptionDetailsIncomeSource),
            accountingPeriodDate = Some(SubscriptionDetailsAccountingPeriodDates)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.AccountingPeriodDate, userInput)

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
