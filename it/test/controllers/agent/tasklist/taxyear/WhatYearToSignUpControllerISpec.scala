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

package controllers.agent.tasklist.taxyear

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.PrePopulate
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.testAccountingYearCurrent
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import models.{Current, EligibilityStatus, Next}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.SelectedTaxYear
import utilities.agent.TestConstants.testUtr
import utilities.{AccountingPeriodUtil, UserMatchingSessionUtil}

import java.time.LocalDate

class WhatYearToSignUpControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/what-year-to-sign-up" when {
    "the Subscription Details Connector returns some data" should {
      "return OK and show the What Tax Year To Sign Up page with current tax year radio selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(Some(testAccountingYearCurrent)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        val fromYear: String = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) - 1).toString
        val toYear: String = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString

        val expectedText = removeHtmlMarkup(messages("agent.business.what-year-to-sign-up.option-1", fromYear, toYear))
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the What Year To Sign Up page")
        res must have(
          httpStatus(OK),
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
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Mandated, Voluntary)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.accountingYear(Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          ))

          Then("Should return SEE_OTHER to task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.taskListURI)
          )
        }
        "The user is eligible for the next tax year only" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true)))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.accountingYear(Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          ))

          Then("Should return SEE_OTHER to task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.taskListURI)
          )
        }
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the What Year To Sign Up page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

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

    "user is unauthorised" should {
      "return SEE_OTHER (303)" in {
        Given("I set up the Wiremock stubs")
        AuthStub.stubUnauthorised()

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        Then("Should return SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/business/what-year-to-sign-up"))
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/what-year-to-sign-up" should {
    "redirect to the Tax Year CYA page" when {
      "not in edit mode" when {
        "selecting the Current Year radio button" in {
          val userInput = Current
          disable(PrePopulate)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

          When("POST /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.taxYearCheckYourAnswersURI)
          )
        }
      }

      "in edit mode" when {
        "selecting the Next radio button" in {
          val userInput = Next
          disable(PrePopulate)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))


          When("POST /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.taxYearCheckYourAnswersURI)
          )
        }
      }
    }

    "redirect to the What You Need To Do page" when {
      "PrePopluate is enabled and not in edit mode" when {
        "selecting the Current Year radio button" in {
          val userInput = Current
          enable(PrePopulate)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

          When("POST /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.whatYouNeedToDoURI)
          )
        }
      }
      "selecting the Next radio button" in {
        val userInput = Next
        enable(PrePopulate)
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.whatYouNeedToDoURI)
        )
      }
    }

    "redirect to the Global CYA page" when {
      "PrePopluate is enabled and in edit mode" when {
        "selecting the Current Year radio button" in {
          val userInput = Current
          enable(PrePopulate)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

          When("POST /client/business/what-year-to-sign-up is called")
          val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.globalCheckYourAnswersURI)
          )
        }
      }
      "selecting the Next radio button" in {
        val userInput = Next
        enable(PrePopulate)
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[AccountingYearModel](SelectedTaxYear, AccountingYearModel(userInput))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, request = Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Tax Year CYA")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.globalCheckYourAnswersURI)
        )
      }
    }

    "return BAD_REQUEST" when {
      "no option has been selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

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
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

        Then("Should return an INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "return SEE_OTHER (303) when user is unauthorised" in {
      Given("I set up the Wiremock stubs")
      AuthStub.stubUnauthorised()

      When("POST /client/business/what-year-to-sign-up is called")
      val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, request = Some(Current))

      Then("Should return SEE_OTHER")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(basGatewaySignIn("/client/business/what-year-to-sign-up"))
      )

    }
  }
}
