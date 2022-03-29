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
import helpers.IntegrationTestConstants.{accountingMethodPropertyURI, checkYourAnswersURI, ukPropertyCYAURI}
import helpers.IntegrationTestModels.{subscriptionData, testFullPropertyModel, testPropertyStartDate}
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.DateModel
import models.common.{IncomeSourceModel, PropertyModel}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys.Property

import java.time.LocalDate

class UkPropertyStartDateISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/property-start-date" when {

    "the Subscription Details Connector returns all data" should {
      "show the property start date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        When("GET /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyStartDate()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property start page with populated start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title") + serviceNameGovUk),
          govukDateField("startDate", testPropertyStartDate.startDate)
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the property start date page" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = true)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
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

  "POST /report-quarterly/income-and-expenses/sign-up/property-start-date" when {
    "not in edit mode" when {
      "save and retrieve is enabled" when {
        "enter start date" should {
          "redirect to the uk property accounting method page" in {
            enable(SaveAndRetrieve)
            val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyStartDate, userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDate = Some(userInput)))

            When("POST /property-start-date is called")
            val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = false, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(accountingMethodPropertyURI)
            )
          }
        }
      }

      "save and retrieve is disabled" when {
        "enter start date" should {
          "redirect to the uk property accounting method page" in {
            enable(SaveAndRetrieve)
            val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyStartDate, userInput)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDate = Some(userInput)))

            When("POST /property-start-date is called")
            val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = false, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(accountingMethodPropertyURI)
            )
          }
        }
      }

      "do not enter start date" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select start date within 12 months" in {
        val userInput: DateModel = IntegrationTestModels.testInvalidPropertyStartDate.startDate
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" when {
      "save and retrieve is enabled" when {
        "simulate not changing the start date" should {
          "redirect to uk property check your answers page" in {
            enable(SaveAndRetrieve)
            val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testFullPropertyModel)

            When("POST /property-start-date is called")
            val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = true, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of check your answers")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(ukPropertyCYAURI)
            )
          }
        }

        "simulate changing the start date" should {
          "redirect to uk property check your answers page" in {
            enable(SaveAndRetrieve)
            val SubscriptionDetailsStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
            val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel.copy(startDate = Some(SubscriptionDetailsStartDate))))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testFullPropertyModel)

            When("POST /property-start-date is called")
            val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = true, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of check your answers")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(ukPropertyCYAURI)
            )
          }
        }
      }

      "save and retrieve is disabled" when {
        "simulate not changing the start date" should {
          "redirect to final check your answers page" in {
            val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testFullPropertyModel)

            When("POST /property-start-date is called")
            val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = true, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of check your answers")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(checkYourAnswersURI)
            )
          }
        }

      }
    }
  }

}
