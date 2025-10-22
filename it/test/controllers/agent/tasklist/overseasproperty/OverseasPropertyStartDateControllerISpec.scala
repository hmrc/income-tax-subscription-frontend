/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.agent.tasklist.overseasproperty

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{AgentURI, testNino, testUtr}
import helpers.IntegrationTestModels
import helpers.IntegrationTestModels._
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.DateModel
import models.common.OverseasPropertyModel
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.OverseasProperty

class OverseasPropertyStartDateControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-start-date" when {
    "the Subscription Details Connector returns all data" should {
      "show the overseas property commencement date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullPropertyModel))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /overseas-property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyStartDate()
        val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
        Then("Should return a OK with the overseas property commencement page with populated commencement date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas-property.start-date.heading") + serviceNameGovUk),
          govukDateField("startDate", testValidStartDate)
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the overseas property commencement date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /overseas-property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyStartDate()
        val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
        Then("Should return a OK with the overseas property commencement date page with no commencement date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas-property.start-date.heading") + serviceNameGovUk),
          govukDateField("startDate", DateModel("", "", ""))
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-start-date" should {
    "redirect to overseas property check your answers page" when {
      "not in edit mode" when {
        "enter commencement date" in {
          val userInput: DateModel = IntegrationTestModels.testValidStartDate
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(OverseasPropertyModel(startDate = Some(userInput)))
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of the foreign property check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.overseasPropertyCheckYourAnswersURI)
          )
        }
      }
      "in edit mode" when {
        "not changing commencement date when calling page from agent Overseas Property Check Your Answers" in {
          val userInput: DateModel = IntegrationTestModels.testValidStartDate
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = OverseasPropertyModel(
            startDate = Some(IntegrationTestModels.testValidStartDate)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(testProperty)
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of agent uk property check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.overseasPropertyCheckYourAnswersURI)

          )
        }
      }
    }

    "return BAD_REQUEST" when {
      "not entering commencement date" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = true, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "selecting commencement date earlier than the start date limit" in {
        val userInput: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "selecting commencement date within 7 days from current date" in {
        val userInput: DateModel = IntegrationTestModels.testInvalidStartDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the start date cannot be saved" in {
        val userInput: DateModel = IntegrationTestModels.testValidStartDate
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(OverseasProperty)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
