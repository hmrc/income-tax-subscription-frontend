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

package controllers.individual.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.AuthStub
import models.DateModel
import models.common.{IncomeSourceModel, OverseasPropertyModel}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys.OverseasProperty

import java.time.LocalDate

class OverseasPropertyStartDateControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/overseas-property-start-date" when {
    "Subscription Details returns all data" should {
      "show the overseas property start date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          OverseasProperty,
          OK,
          Json.toJson(OverseasPropertyModel(startDate = Some(testPropertyStartDate.startDate)))
        )

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyStartDate()

        Then("Should return a OK with the overseas property start page with populated commencement date")
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        res should have(
          httpStatus(OK),
          pageTitle(messages("overseas.property.name.title") + serviceNameGovUk),
          govukDateField("startDate", testPropertyStartDate.startDate)
        )
      }
    }

    "Subscription Details returns no data" should {
      "show the overseas property start date page" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = true)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyStartDate()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the overseas property start date page with no commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("overseas.property.name.title") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/overseas-property-start-date" when {
    "not in edit mode" when {
      "enter commencement date" should {
        "redirect to the overseas property accounting method page" in {
          val userInput = testValidStartDate
          val expected = OverseasPropertyModel(startDate = Some(userInput))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of overseas property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodOverseasPropertyURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
        }
      }

      "do not enter commencement date" should {
        "return BAD_REQUEST" in {
          Given("I setup the Wiremock stubs")
          val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
            foreignProperty = false)
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, None)

          Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
          res should have(
            httpStatus(BAD_REQUEST),
            errorDisplayed()
          )
        }
      }

      "select commencement date within 12 months" should {
        "return BAD_REQUEST" in {
          val userInput = testInvalidStartDate
          val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false,
            foreignProperty = true)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
          res should have(
            httpStatus(BAD_REQUEST),
            errorDisplayed()
          )
        }
      }
    }

    "in edit mode" when {
      "enter the same start date" should {
        "redirect to the check your answers page" in {
          val userInput = testValidStartDate
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
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
        }
      }

      "enter a new start date" should {
        "redirect to the check your answers page" in {
          val subscriptionDetailsStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
          val userInput = testValidStartDate
          val expected = OverseasPropertyModel(startDate = Some(userInput))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            OverseasProperty,
            OK,
            Json.toJson(OverseasPropertyModel(startDate = Some(subscriptionDetailsStartDate)))
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
        }
      }
    }
  }
}
