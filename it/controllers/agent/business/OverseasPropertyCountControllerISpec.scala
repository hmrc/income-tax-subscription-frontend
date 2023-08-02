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
import forms.agent.OverseasPropertyCountForm
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{accountingMethodOverseasPropertyURI, overseasPropertyCYAURI}
import helpers.IntegrationTestModels.testFullOverseasPropertyModel
import helpers.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty

class OverseasPropertyCountControllerISpec extends ComponentSpecBase {

  s"GET ${routes.OverseasPropertyCountController.show().url}" should {

    "show the overseas property count" when {
      "the Subscription Details Connector returns all data" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When(s"GET ${routes.OverseasPropertyCountController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyCount()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        Then("Should return a OK with the property start page with populated start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.overseas-property.count.heading") + serviceNameGovUk),
          textField(OverseasPropertyCountForm.fieldName, "1")
        )
      }

      "the Subscription Details Connector returns no data" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When(s"GET ${routes.OverseasPropertyCountController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyCount()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

        Then("Should return a OK with the property start date page with no start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("business.overseas-property.count.heading") + serviceNameGovUk),
          textField(OverseasPropertyCountForm.fieldName, "")
        )
      }
    }
  }

  s"POST ${routes.OverseasPropertyCountController.submit().url}" should {
    "redirect to the overseas property accounting method page" when {
      "not in edit mode" when {
        "a valid count is input" in {
          val userInput: Int = 1

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(OverseasPropertyModel(count = Some(userInput)))

          When(s"POST ${routes.OverseasPropertyCountController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCount(isEditMode = false, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of overseas property accounting method page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodOverseasPropertyURI)
          )
        }
      }
    }

    "redirect to overseas property check your answers page" when {
      "in edit mode" when {
        "not changing the count" in {
          val userInput: Int = 1
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = OverseasPropertyModel(
            count = Some(1)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(testProperty)

          When("POST /property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCount(isEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(overseasPropertyCYAURI)
          )
        }

        "changing the count" in {
          val userInput: Int = 2

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = OverseasPropertyModel(
            count = Some(1)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(testProperty.copy(count = Some(2)))

          When(s"POST ${routes.OverseasPropertyCountController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCount(isEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(overseasPropertyCYAURI)
          )
        }
      }
    }

    "return BAD_REQUEST" when {
      "not entering a count" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCount(isEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "entering a count of zero" in {
        val userInput: Int = 0

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCount(isEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "return INTERNAL_SERVER_ERROR" when {
      "the count cannot be saved" in {
        val userInput: Int = 1

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(OverseasProperty)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCount(isEditMode = false, Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
