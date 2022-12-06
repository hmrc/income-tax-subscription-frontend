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

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.testFullPropertyModel
import helpers.servicemocks.AuthStub
import models.common.PropertyModel
import models.{Accruals, Cash}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.Property

class PropertyAccountingMethodControllerISpec extends ComponentSpecBase  {

  "GET /report-quarterly/income-and-expenses/sign-up/business/accounting-method-property" when {

    "the Subscription Details Connector returns pre-populated data" should {
      "show the accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("summary.income_type.cash"))
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property accounting method page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("property.accounting-method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "the Subscription Details Connector returns with no pre-populated data" should {
      "show the property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("GET /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.propertyAccountingMethod()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the property accounting method page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("property.accounting-method.title") + serviceNameGovUk),
          radioButtonSet(id = "propertyAccountingMethod", selectedRadioButton = None)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/accounting-method-property" should {
    "redirect to the property CYA page" when {
      "not in edit mode" when {
        "selecting the Cash radio button on the property accounting method page" in {
          val userInput = Cash

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(accountingMethod = Some(userInput)))

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(ukPropertyCYAURI)
          )
        }

        "selecting the Accruals radio button on the accounting method page" in {
          val userInput = Accruals

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = PropertyModel(
            accountingMethod = Some(Accruals)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testProperty)

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(ukPropertyCYAURI)
          )
        }
      }

      "in edit mode" when {
        "selecting the Cash radio button on the property accounting method page" in {
          val userInput = Cash

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = PropertyModel(
            accountingMethod = Some(Accruals)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testProperty.copy(accountingMethod = Some(userInput)))

          When("POST /business/accounting-method-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(ukPropertyCYAURI)
          )
        }
      }
    }

    "return BAD_REQUEST" when {
      "not selecting an option on the property accounting method page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST /business/accounting-method-property is called")

        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the account method cannot be saved" in {
        val userInput = Accruals

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(Property)

        When("POST /business/accounting-method-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
