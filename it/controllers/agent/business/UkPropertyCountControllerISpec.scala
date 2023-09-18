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
import forms.agent.UkPropertyCountForm
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.{propertyAccountingMethodURI, ukPropertyCheckYourAnswersURI}
import helpers.IntegrationTestModels.testFullPropertyModel
import helpers.agent.servicemocks.AuthStub
import models.common.PropertyModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.Property

class UkPropertyCountControllerISpec extends ComponentSpecBase {

  s"GET ${routes.UkPropertyCountController.show().url}" should {

    "show the uk property count" when {
      "the Subscription Details Connector returns all data" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testFullPropertyModel))

        When(s"GET ${routes.UkPropertyCountController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyCount()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the property start page with populated start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.property.count.heading") + serviceNameGovUk),
          textField(UkPropertyCountForm.fieldName, "1")
        )
      }

      "the Subscription Details Connector returns no data" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When(s"GET ${routes.UkPropertyCountController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyCount()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the property start date page with no start date")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.business.property.count.heading") + serviceNameGovUk),
          textField(UkPropertyCountForm.fieldName, "")
        )
      }
    }
  }

  s"POST ${routes.UkPropertyCountController.submit().url}" should {
    "redirect to the uk property accounting method page" when {
      "not in edit mode" when {
        "a valid count is input" in {
          val userInput: Int = 1

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(count = Some(userInput)))

          When(s"POST ${routes.UkPropertyCountController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyCount(isEditMode = false, request = Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyAccountingMethodURI)
          )
        }
      }
    }

    "redirect to uk property check your answers page" when {
      "in edit mode" when {
        "not changing the count" in {
          val userInput: Int = 1
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = PropertyModel(
            count = Some(1)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testProperty)

          When("POST /property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyCount(isEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(ukPropertyCheckYourAnswersURI)
          )
        }

        "changing the count" in {
          val userInput: Int = 2

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          val testProperty = PropertyModel(
            count = Some(1)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testProperty.copy(count = Some(2)))

          When(s"POST ${routes.UkPropertyCountController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyCount(isEditMode = true, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of check your answers")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(ukPropertyCheckYourAnswersURI)
          )
        }
      }
    }

    "return BAD_REQUEST" when {
      "not entering a count" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyCount(isEditMode = false, None)

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
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyCount(isEditMode = false, Some(userInput))

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
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(Property)

        When("POST /property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyCount(isEditMode = false, Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
