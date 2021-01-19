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

package controllers.individual.business

import java.time.LocalDate

import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.subscriptionData
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.common.AccountingYearModel
import models.{Current, Next}
import play.api.http.Status._
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

class WhatYearToSignUpControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /report-quarterly/income-and-expenses/sign-up/business/what-year-to-sign-up" when {

    "the Subscription Details Connector returns some data" should {
      "show the What Tax Year To Sign Up with an option current tax year selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

        When("GET /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        val fromYear: String = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) - 1).toString
        val toYear: String = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString

        val expectedText = removeHtmlMarkup(messages("business.what_year_to_sign_up.option_1.signup", fromYear, toYear))
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the What Year To Sign Up page")
        res should have(
          httpStatus(200),
          pageTitle(messages("business.what_year_to_sign_up.title.signup") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear", selectedRadioButton = Some(expectedText)),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the What Year To Sign Up page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(selectedTaxYear = None))

        When("GET /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the What Year To Sign Up page")
        res should have(
          httpStatus(200),
          pageTitle(messages("business.what_year_to_sign_up.title.signup") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear", selectedRadioButton = None),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/what-year-to-sign-up" when {

    "not in edit mode" should {

      "select the Current Year radio button on the What Year To Sign Up page" in {
        val userInput = AccountingYearModel(Current)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(selectedTaxYear = None))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, userInput)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Business Accounting Period Method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeReceivedURI)
        )
      }

      "select the Next radio button on the What Year To Sign Up page" in {
        val userInput = AccountingYearModel(Next)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(selectedTaxYear = None))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, userInput)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Business Accounting Period Method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeReceivedURI)
        )
      }
    }

    "not select an option on the accounting year page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, "")

      When("POST /business/what-year-to-sign-up is called")

      val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "in edit mode" should {

      "changing from the Current radio button to Next on the accounting method page" in {


        val SubscriptionDetailsAccountingYearCurrent: AccountingYearModel = IntegrationTestModels.testAccountingYearCurrent
        val SubscriptionDetailsAccountingYearNext: AccountingYearModel = IntegrationTestModels.testAccountingYearNext
        val userInput = SubscriptionDetailsAccountingYearNext

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(selectedTaxYear = Some(SubscriptionDetailsAccountingYearCurrent))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, userInput)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of CYA")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing accounting year when calling page from Check Your Answers" in {


        val SubscriptionDetailsAccountingYearCurrent: AccountingYearModel = IntegrationTestModels.testAccountingYearCurrent
        val userInput = SubscriptionDetailsAccountingYearCurrent

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            selectedTaxYear = Some(SubscriptionDetailsAccountingYearCurrent)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, userInput)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }
  }
}
