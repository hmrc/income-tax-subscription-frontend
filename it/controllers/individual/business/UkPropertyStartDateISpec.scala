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

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants.{accountingMethodPropertyURI, checkYourAnswersURI}
import helpers.IntegrationTestModels.{subscriptionData, testPropertyStartDate}
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.DateModel
import models.common.{IncomeSourceModel, PropertyStartDateModel}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import utilities.SubscriptionDataKeys

class UkPropertyStartDateISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/property-start-date" when {

    "the Subscription Details Connector returns all data" should {
      "show the property start date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

        When("GET /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyStartDate()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property start page with populated start date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title") + serviceNameGovUk),
          dateField("startDate", testPropertyStartDate.startDate)
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

        When("GET /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyStartDate()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property start date page with no start date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title") + serviceNameGovUk)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/property-start-date" when {
    "not in edit mode" when {
      "enter start date" should {
        "redirect to the accounting method page" in {
          val userInput: PropertyStartDateModel = IntegrationTestModels.testPropertyStartDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyStartDate, userInput)

          When("POST /property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
        }
      }

      "do not enter start date" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyStartDate, "")

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select start date within 12 months" in {
        val userInput: PropertyStartDateModel = IntegrationTestModels.testInvalidPropertyStartDate
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.BusinessName, userInput)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing start date when calling page from Check Your Answers" in {
        val userInput: PropertyStartDateModel = IntegrationTestModels.testPropertyStartDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyStartDate, userInput)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing start date when calling page from Check Your Answers" in {
        val SubscriptionDetailsStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
        val SubscriptionDetailsPropertyStartDate = PropertyStartDateModel(SubscriptionDetailsStartDate)
        val userInput: PropertyStartDateModel = IntegrationTestModels.testPropertyStartDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            propertyStartDate = Some(SubscriptionDetailsPropertyStartDate)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyStartDate, userInput)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyStartDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
