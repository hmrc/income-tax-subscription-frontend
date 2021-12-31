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

package controllers.agent.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestModels.testFullOverseasPropertyModel
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.checkYourAnswersURI
import helpers.agent.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import models.{Accruals, Cash}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty

class OverseasPropertyAccountingMethodControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-accounting-method" when {

    "Subscription details returns pre-populated data" should {
      "show the foreign property accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When("GET /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("agent.overseas.property.accounting_method.radio.cash"))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the foreign property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodOverseasProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "Subscription details returns with no pre-populated data" should {
      "show the foreign property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When("GET /business/overseas-property-accounting-method is called")

        val res = IncomeTaxSubscriptionFrontend.overseasPropertyAccountingMethod()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the foreign property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "foreignPropertyAccountingMethod", selectedRadioButton = None)
        )
      }
    }
  }

  "POST /business/accounting-method-property" when {
    "not in Edit Mode" should {
      "select the Cash radio button on the Overseas Property Accounting Method page" in {
        val userInput = Cash
        val expected = OverseasPropertyModel(accountingMethod = Some(userInput))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
      }

      "select the Accruals radio button on the Overseas Property Accounting Method page" in {
        val userInput = Accruals
        val expected = OverseasPropertyModel(accountingMethod = Some(userInput))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
      }

      "not select a radio button on the Overseas Property Accounting Method page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }
    }

    "in Edit Mode" should {
      "changing to the Accruals radio button on the overseas property accounting method page" in {
        val userInput = Accruals
        val expected = OverseasPropertyModel(accountingMethod = Some(userInput))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          OverseasProperty,
          OK,
          Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash)))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
      }

      "not changing the radio button on the overseas property accounting method page" in {
        val userInput = Cash
        val expected = OverseasPropertyModel(accountingMethod = Some(userInput))

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          OverseasProperty,
          OK,
          Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash)))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = true, Some(userInput))

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
