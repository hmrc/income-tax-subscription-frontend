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

package controllers.agent.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestModels
import helpers.IntegrationTestModels.{testInvalidStartDate, testPropertyStartDate}
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels.testValidStartDate
import helpers.agent.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty

class OverseasPropertyStartDateControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-start-date" when {
    "Subscription Details returns all data" should {
      "show the Overseas property Start date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          OverseasProperty,
          OK,
          Json.toJson(OverseasPropertyModel(startDate = Some(testPropertyStartDate.startDate)))
        )

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyStartDate()

        Then("Should return a OK with the Overseas property Start page with populated start date")
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.name.heading") + serviceNameGovUk),
          govukDateField("startDate", testPropertyStartDate.startDate)
        )
      }
    }

    "Subscription Details returns no data" should {
      "show the Overseas property Start date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyStartDate()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the Overseas property Start date page with no start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.name.heading") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-start-date" should {
    "redirect to the Overseas property accounting method page" when {
      "not in edit mode" in {
        val userInput = IntegrationTestModels.testValidStartDate
        val expected = OverseasPropertyModel(startDate = Some(userInput))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Overseas property accounting method page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyAccountingMethod)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
      }

      "redirect to the agent overseas property check your answers page" when {
        "in edit mode" when {
          "enter the same start date" in {
            val userInput = IntegrationTestModels.testValidStartDate
            val expected = OverseasPropertyModel(startDate = Some(userInput))

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              OverseasProperty,
              OK,
              Json.toJson(expected)
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

            When("POST /overseas-property-start-date is called")
            val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = true, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of check your answers")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(overseasPropertyCheckYourAnswersURI)
            )

            IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
          }
        }

        "enter a new start date" in {
          val userInput = IntegrationTestModels.testValidStartDate2
          val expected = OverseasPropertyModel(startDate = Some(userInput))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(OverseasPropertyModel(startDate = Some(testPropertyStartDate.startDate)))
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(overseasPropertyCheckYourAnswersURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
        }
      }

      "return BAD_REQUEST" when {
        "do not enter start date" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, None)

          Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
          res must have(
            httpStatus(BAD_REQUEST),
            errorDisplayed()
          )
        }

        "select start date within 12 months" in {
          val userInput = testInvalidStartDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

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
        "cannot save the start date" in {
          val userInput = testValidStartDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(OverseasProperty)

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }
}
