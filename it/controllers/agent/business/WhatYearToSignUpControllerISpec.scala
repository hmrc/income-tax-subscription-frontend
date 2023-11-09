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

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.ForeignProperty
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants.{testFirstName, testLastName}
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels.testAccountingYearCurrent
import helpers.agent.servicemocks.AuthStub
import models.common.AccountingYearModel
import models.{Current, Next}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.{AccountingPeriodUtil, UserMatchingSessionUtil}
import utilities.SubscriptionDataKeys.SelectedTaxYear

import java.time.LocalDate

class WhatYearToSignUpControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(ForeignProperty)
    super.beforeEach()
  }


  "GET /report-quarterly/income-and-expenses/sign-up/client/business/what-year-to-sign-up" when {
    "the Subscription Details Connector returns some data" should {
      "show the What Tax Year To Sign Up with an option current tax year selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(Some(testAccountingYearCurrent)))

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        val fromYear: String = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) - 1).toString
        val toYear: String = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString

        val expectedText = removeHtmlMarkup(messages("agent.business.what-year-to-sign-up.option-1", fromYear, toYear))
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the What Year To Sign Up page")
        res must have(
          httpStatus(200),
          pageTitle(messages("agent.business.what-year-to-sign-up.heading") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear", selectedRadioButton = Some(expectedText)),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
      "return SEE_OTHER" when {
        "The user is mandated for the current tax year" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

          When("GET /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers(Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName,
            ITSASessionKeys.NINO -> testNino,
            ITSASessionKeys.MANDATED_CURRENT_YEAR -> "true"
          ))

          Then("Should return SEE_OTHER to task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )
        }
        "The user is eligible for the next tax year only" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

          When("GET /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.getTaxYearCheckYourAnswers(Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName,
            ITSASessionKeys.NINO -> testNino,
            ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "true"
          ))

          Then("Should return SEE_OTHER to task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )
        }
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the What Year To Sign Up page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the What Year To Sign Up page")
        res must have(
          httpStatus(200),
          pageTitle(messages("agent.business.what-year-to-sign-up.heading") + serviceNameGovUk),
          radioButtonSet(id = "accountingYear", selectedRadioButton = None),
          radioButtonSet(id = "accountingYear-2", selectedRadioButton = None)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up//client/business/what-year-to-sign-up" should {
    "redirect to the Tax Year CYA page" when {
      "not in edit mode" when {
        "selecting the Current Year radio button" in {
          val userInput = Current

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))

          When("POST /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(taxYearCheckYourAnswersURI)
          )
        }
      }

      "in edit mode" when {
        "selecting the Next radio button" in {
          val userInput = Next

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))

          When("POST /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(taxYearCheckYourAnswersURI)
          )
        }
      }
    }

    "return BAD_REQUEST" when {
      "no option has been selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)

        When("POST /client/business/what-year-to-sign-up is called")

        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "there is a failure while saving the tax year" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(SelectedTaxYear)

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

        Then("Should return an INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
