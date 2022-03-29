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

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.subscriptionData
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.common.AccountingYearModel
import models.{AccountingYear, Current, Next}
import play.api.http.Status._
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

import java.time.LocalDate

class WhatYearToSignUpControllerISpec extends ComponentSpecBase  {

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

        val expectedText = removeHtmlMarkup(messages("business.what_year_to_sign_up.option_1", fromYear, toYear))
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the What Year To Sign Up page")
        res must have(
          httpStatus(200),
          pageTitle(messages("business.what_year_to_sign_up.title") + serviceNameGovUk),
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
        res must have(
          httpStatus(200),
          pageTitle(messages("business.what_year_to_sign_up.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear", selectedRadioButton = None),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/what-year-to-sign-up" when {

    "not in edit mode" should {

      "select the Current Year radio button on the What Year To Sign Up page" in {
        val userInput = Current

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(selectedTaxYear = None))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYear](SubscriptionDataKeys.SelectedTaxYear, userInput)

        And("SaveAndRetrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Business Accounting Period Method page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeReceivedURI)
        )
      }

      "select the Next radio button on the What Year To Sign Up page" in {
        val userInput = Next

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(selectedTaxYear = None))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYear](SubscriptionDataKeys.SelectedTaxYear, userInput)

        And("SaveAndRetrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Business Accounting Period Method page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeReceivedURI)
        )
      }

      "Save & Retrieve is enabled" in {
        val userInput = Current

        Given("I setup the Wiremock stubs")

        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(selectedTaxYear = None))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYear](SubscriptionDataKeys.SelectedTaxYear, userInput)

        And("SaveAndRetrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Task List page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taxYearCyaURI)
        )
      }
    }

    "not select an option on the accounting year page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, "")

      And("SaveAndRetrieve feature switch is disabled")
      disable(SaveAndRetrieve)

      When("POST /business/what-year-to-sign-up is called")

      val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res must have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "in edit mode" should {

      "changing from the Current radio button to Next on the accounting method page" in {


        val SubscriptionDetailsAccountingYearCurrent: AccountingYearModel = IntegrationTestModels.testAccountingYearCurrent
        val userInput = Next

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(selectedTaxYear = Some(SubscriptionDetailsAccountingYearCurrent))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYear](SubscriptionDataKeys.SelectedTaxYear, userInput)

        And("SaveAndRetrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of CYA")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing accounting year when calling page from Check Your Answers" in {


        val SubscriptionDetailsAccountingYearCurrent: AccountingYearModel = IntegrationTestModels.testAccountingYearCurrent
        val userInput = Current

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            selectedTaxYear = Some(SubscriptionDetailsAccountingYearCurrent)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYear](SubscriptionDataKeys.SelectedTaxYear, userInput)

        And("SaveAndRetrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "Save & Retrieve is enabled" in {
        val SubscriptionDetailsAccountingYearCurrent: AccountingYearModel = IntegrationTestModels.testAccountingYearCurrent
        val userInput = Next

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(selectedTaxYear = Some(SubscriptionDetailsAccountingYearCurrent))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYear](SubscriptionDataKeys.SelectedTaxYear, userInput)

        And("SaveAndRetrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("POST /business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Task List page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taxYearCyaURI)
        )
      }
    }
  }
}
