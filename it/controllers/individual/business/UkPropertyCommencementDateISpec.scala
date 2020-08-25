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
import helpers.IntegrationTestModels.{subscriptionData, testPropertyCommencementDate}
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.DateModel
import models.individual.business.PropertyCommencementDateModel
import models.individual.incomesource.IncomeSourceModel
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import utilities.SubscriptionDataKeys

class UkPropertyCommencementDateISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/property-commencement-date" when {

    "the Subscription Details Connector returns all data" should {
      "show the property commencement date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubIndivFullSubscriptionBothPost()

        When("GET /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyCommencementDate()

        Then("Should return a OK with the property commencement page with populated commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title")),
          dateField("startDate", testPropertyCommencementDate.startDate)
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the property commencement date page" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = true)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(individualIncomeSource = Some(incomeSourceModel)))

        When("GET /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyCommencementDate()

        Then("Should return a OK with the property commencement date page with no commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title"))
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/property-commencement-date" when {
    "not in edit mode" when {
      "enter commencement date" should {
        "redirect to the accounting method page" in {
          val userInput: PropertyCommencementDateModel = IntegrationTestModels.testPropertyCommencementDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyCommencementDate, userInput)

          When("POST /property-commencement-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
        }
      }

      "do not enter commencement date" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(individualIncomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyCommencementDate, "")

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select commencement date within 12 months" in {
        val userInput: PropertyCommencementDateModel = IntegrationTestModels.testInvalidCommencementDate
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(individualIncomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.BusinessName, userInput)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing commencement date when calling page from Check Your Answers" in {
        val userInput: PropertyCommencementDateModel = IntegrationTestModels.testPropertyCommencementDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyCommencementDate, userInput)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing commencement date when calling page from Check Your Answers" in {
        val kSubscriptionDetailsCommencementDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
        val kSubscriptionDetailsPropertyCommencementDate = PropertyCommencementDateModel(kSubscriptionDetailsCommencementDate)
        val userInput: PropertyCommencementDateModel = IntegrationTestModels.testPropertyCommencementDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            propertyCommencementDate = Some(kSubscriptionDetailsPropertyCommencementDate)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyCommencementDate, userInput)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
