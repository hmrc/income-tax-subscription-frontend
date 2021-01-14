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

import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels._
import helpers.agent.servicemocks.AuthStub
import models.common.{AccountingPeriodModel, _}
import models.{Accruals, Cash}
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class PropertyAccountingMethodControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /business/accounting-method-property" when {
    "the Subscription Details Connector returns all data" should {
      "show the property accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("agent.property.accounting_method.radio.cash"))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "the Subscription Details Connector returns no property accounting method" should {
      "show the property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
        ))

        When("GET /business/property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = None)
        )
      }
    }
  }

  "POST /business/accounting-method-property" when {
    "not in edit mode" when {
      "the user does not have foreign income" should {
        "select the Cash radio button on the Property Accounting Method page" in {
          val userInput = AccountingMethodPropertyModel(Cash)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
          ))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Check Your Answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }

        "select the Accruals radio button on the Property Accounting Method page" in {
          val userInput = AccountingMethodPropertyModel(Accruals)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
          ))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Check Your Answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user has foreign income" should {
        "select an option on the property accounting method page" in {
          val userInput = AccountingMethodPropertyModel(Cash)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
          ))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, userInput)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Check Your Answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(overseasPropertyStartDateURI)
          )
        }
      }

      "not select an option on the Property Accounting Method page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
          incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
        ))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, "")

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }
    }
    "in edit mode" should {
      "changing to the Accruals radio button on the accounting method page" in {
        val SubscriptionDetailsIncomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
        val SubscriptionDetailsAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
        val userInput = AccountingMethodPropertyModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            incomeSource = Some(SubscriptionDetailsIncomeSource),
            accountingMethodProperty = Some(SubscriptionDetailsAccountingMethodProperty)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, userInput)

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }
}

