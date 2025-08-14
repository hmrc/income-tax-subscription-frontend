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

package controllers.individual.tasklist.ukproperty

import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants.IndividualURI
import helpers.IntegrationTestModels.testFullPropertyModel
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.DateModel
import models.common.PropertyModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.Property

class PropertyStartDateControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(RemoveAccountingMethod)
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/property-start-date" should {

    "show the property start date page" when {
      "the Subscription Details Connector returns all data" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        When("GET /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyStartDate()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property start page with populated start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title") + serviceNameGovUk),
          govukDateField("startDate", IntegrationTestModels.testValidStartDate)
        )
      }

      "the Subscription Details Connector returns no data" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("GET /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyStartDate()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property start date page with no start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title") + serviceNameGovUk),
          govukDateField("startDate", DateModel("", "", ""))
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/property-start-date" should {
    "redirect to the uk property accounting method page" when {
      "not in edit mode" when {
        "enter a valid start date" in {
          val userInput: DateModel = IntegrationTestModels.testValidStartDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDate = Some(userInput)))
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

          When("POST /property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.accountingMethodPropertyURI)
          )
        }
      }
    }

    "redirect to the overseas property check your answers page" when {
      "not in edit mode" when {
        "feature switch is enabled" in {
          enable(RemoveAccountingMethod)

          val userInput = IntegrationTestModels.testValidStartDate
          val expected   = PropertyModel(startDate = Some(userInput))

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            NO_CONTENT
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(expected)
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should skip the accounting method page and go straight to check-your-answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.ukPropertyCYAURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveProperty(expected, Some(1))
        }
      }
    }

    "redirect to uk property check your answers page" when {
      "in edit mode" when {
        "not changing the start date" in {
          val userInput: DateModel = IntegrationTestModels.testValidStartDate
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = PropertyModel(
            startDate = Some(IntegrationTestModels.testValidStartDate)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testProperty)
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

          When("POST /property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.ukPropertyCYAURI)
          )
        }

        "changing the start date" in {
          val userInput: DateModel = IntegrationTestModels.testValidStartDate2

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = PropertyModel(
            startDate = Some(IntegrationTestModels.testValidStartDate)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testProperty.copy(startDate = Some(IntegrationTestModels.testValidStartDate2)))
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

          When("POST /property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.ukPropertyCYAURI)
          )
        }
      }
    }

    "return BAD_REQUEST" when {
      "not entering start date" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "selecting start date within 7 days including the current date" in {
        val userInput: DateModel = IntegrationTestModels.testInvalidStartDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "selecting a start date before the 1st January 1900 when the start date before limit feature switch is disabled" in {
        val userInput: DateModel = DateModel("31", "12", "1899")

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = false, Some(userInput))

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
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(Property)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
