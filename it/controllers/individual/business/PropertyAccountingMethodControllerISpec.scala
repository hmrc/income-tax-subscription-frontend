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
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{subscriptionData, testAccountingMethodProperty}
import helpers.servicemocks.AuthStub
import models.common.AccountingMethodPropertyModel
import models.{Accruals, Cash}
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class PropertyAccountingMethodControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/accounting-method-property" when {

    "the Subscription Details Connector returns pre-populated data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("summary.income_type.cash"))
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "the Subscription Details Connector returns with no pre-populated data" should {
      "show the property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(propertyAccountingMethod = Some(testAccountingMethodProperty)))

        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "propertyAccountingMethod", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/accounting-method-property" when {

    "select the Cash radio button on the property accounting method page" in {
      val userInput = AccountingMethodPropertyModel(Cash)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, userInput)

      When("POST /business/accounting-method-property is called")
      val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

      Then("Should return a SEE_OTHER with a redirect location of check your answers")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(checkYourAnswersURI)
      )
    }

    "select the Accruals radio button on the accounting method page" in {
      val userInput = AccountingMethodPropertyModel(Accruals)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, userInput)

      When("POST /business/accounting-method-property is called")
      val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

      Then("Should return a SEE_OTHER with a redirect location of check your answers")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(checkYourAnswersURI)
      )
    }

    "not select an option on the property accounting method page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, "")

      When("POST /business/accounting-method-property is called")

      val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "save and retrieve feature switch is enabled" when {
      "select the Cash radio button on the property accounting method page" when {
        "click save and continue button" should {
          "redirect to task list page" in {
            enable(SaveAndRetrieve)
            val userInput = AccountingMethodPropertyModel(Cash)

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
            IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.PropertyAccountingMethod, userInput)

            When("POST /business/accounting-method-property is called")
            val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of check your answers")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(taskListURI)
            )
          }
        }
      }
    }
  }
}
