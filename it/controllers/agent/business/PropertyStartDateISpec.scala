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

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import controllers.Assets.NO_CONTENT
import helpers.IntegrationTestModels
import helpers.IntegrationTestModels.{subscriptionData, testFullPropertyModel, testPropertyStartDate}
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.{checkYourAnswersURI, propertyAccountingMethodURI}
import helpers.agent.servicemocks.AuthStub
import models.DateModel
import models.common.{IncomeSourceModel, PropertyModel, PropertyStartDateModel}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.Property

import java.time.LocalDate

class PropertyStartDateISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/property-commencement-date" when {

    "the Subscription Details Connector returns all data" should {
      "show the property commencement date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        When("GET /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyStartDate()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property commencement page with populated commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.property.name.heading") + serviceNameGovUk),
          govukDateField("startDate", testPropertyStartDate.startDate)
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the property commencement date page" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = true)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("GET /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyStartDate()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property commencement date page with no commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.property.name.heading") + serviceNameGovUk),
          govukDateField("startDate", DateModel("", "", ""))
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/property-commencement-date" when {
    "not in edit mode" when {
      "enter commencement date" should {
        "redirect to the accounting method page" in {
          val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDate = Some(userInput)))

          When("POST /property-commencement-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDate(isEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyAccountingMethodURI)
          )
        }
      }

      "do not enter commencement date" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDate(isEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select commencement date within 12 months" in {
        val userInput: DateModel = IntegrationTestModels.testInvalidPropertyStartDate.startDate
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDate(isEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing commencement date when calling page from Check Your Answers" in {
        val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testFullPropertyModel)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDate(isEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing commencement date when calling page from Check Your Answers" in {
        val SubscriptionDetailsStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
        val SubscriptionDetailsPropertyStartDate = PropertyStartDateModel(SubscriptionDetailsStartDate)
        val userInput: DateModel = IntegrationTestModels.testPropertyStartDate.startDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          Property,
          OK,
          Json.toJson(testFullPropertyModel.copy(startDate = Some(SubscriptionDetailsPropertyStartDate.startDate)))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testFullPropertyModel)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDate(isEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
