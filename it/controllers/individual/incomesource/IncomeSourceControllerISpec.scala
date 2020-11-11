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

package controllers.individual.incomesource

import config.featureswitch.FeatureSwitch.{PropertyNextTaxYear, ReleaseFour}
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.AuthStub
import models.common.IncomeSourceModel
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class IncomeSourceControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {

    "the Subscription Details Connector returns all data" should {
      "show the income source page with the options selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("income_source.title") + serviceNameGovUk),
          checkboxSet(id = "Business", selectedCheckbox = Some(messages("income_source.selfEmployed"))),
          checkboxSet(id = "UkProperty", selectedCheckbox = Some(messages("income_source.rentUkProperty")))
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the rent uk property page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the rent uk property page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("income_source.title") + serviceNameGovUk),
          checkboxSet(id = "Business", selectedCheckbox = None),
          checkboxSet(id = "UkProperty", selectedCheckbox = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {
    "not in edit mode and release four is disabled" when {
      "the user is self-employed, doesn't have uk and foreign property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user is self-employed and has uk property but doesn't have foreign property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user is self-employed and has uk and foreign property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user is self-employed and has foreign property but doesn't have uk property " in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
      "the user rents a uk property, is not self-employed and doesn't have foreign property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingMethodPropertyURI)
        )
      }
      "the user rents a uk property and has foreign property but is not self-employed" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)


        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingMethodPropertyURI)
        )
      }
      "the user rents a foreign property, is not self-employed and doesn't have uk property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyCommencementDateURI)
        )
      }
    }

    "in edit mode" when {
      "the user selects a different answer" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property accounting method")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingMethodPropertyURI)
        )
      }
      "the user selects the same answer" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(userInput)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {
    "propertyNextTaxYear is enabled and releaseFour is enabled" when {
      "the user is self-employed, doesn't have uk and foreign property " in {

        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(PropertyNextTaxYear)

        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
      "the user is self-employed and has uk property but doesn't have foreign property" in {

        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(PropertyNextTaxYear)

        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
      "the user is self-employed and has uk and foreign property " in {

        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(PropertyNextTaxYear)

        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
      "the user is self-employed and has foreign property but doesn't have uk property " in {

        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(PropertyNextTaxYear)

        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
      "the user rents a uk property, is not self-employed and doesn't have foreign property" in {

        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(PropertyNextTaxYear)

        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
      "the user rents a uk property and has foreign property but is not self-employed" in {

        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(PropertyNextTaxYear)

        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)


        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
      "the user rents a foreign property, is not self-employed and doesn't have uk property" in {

        Given("I set the required feature switches")
        enable(ReleaseFour)
        enable(PropertyNextTaxYear)

        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(accountingYearURI)
        )
      }
    }
  }
}
