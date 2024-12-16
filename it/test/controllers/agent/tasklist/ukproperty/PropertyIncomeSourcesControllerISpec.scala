/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent.tasklist.ukproperty

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{AgentURI, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.{Cash, DateModel}
import models.common.PropertyModel
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.Property
import utilities.agent.TestConstants.testNino


class PropertyIncomeSourcesControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/your-income-source" when {
    "the Subscription Details Connector returns no data" should {
      "show the property income sources page with empty form" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk)
        )
      }
    }

    "the Subscription Details Connector returns start date only" should {
      "show the property income sources page with start date pre-filled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(startDate = Some(DateModel("10", "10", "2020")))))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk)
        )
      }
    }

    "the Subscription Details Connector returns accounting method only" should {
      "show the property income sources page with accounting method pre-filled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(accountingMethod = Some(Cash))))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk)
        )
      }
    }

    "the Subscription Details Connector returns full data" should {
      "show the property income sources page with full data pre-filled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(
          startDate = Some(DateModel("10", "10", "2020")),
          accountingMethod = Some(Cash)
        )))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/income-sources-property" should {
    "return BAD_REQUEST" when {
      "nothing is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(isEditMode = false, startDate = None, accountingMethod = None)

        Then("Should return a BAD_REQUEST and show the property income sources page with errors")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.property.income-source.heading") + " - Use software to report your client’s Income Tax - GOV.UK")
        )
      }

      "only start date is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
          isEditMode = false,
          startDate = Some(DateModel("10", "10", "2020")),
          accountingMethod = None
        )

        Then("Should return a BAD_REQUEST and show the property income sources page with errors")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.property.income-source.heading") + " - Use software to report your client’s Income Tax - GOV.UK")
        )
      }

      "only accounting method is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
          isEditMode = false,
          startDate = None,
          accountingMethod = Some(Cash)
        )

        Then("Should return a BAD_REQUEST and show the property income sources page with errors")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.property.income-source.heading") + " - Use software to report your client’s Income Tax - GOV.UK")
        )
      }
    }

    "redirect to check your answers page" when {
      "full data is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(Some(Cash), Some(DateModel("10" ,"10" ,"2020"))))
        IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

        When("POST /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
          isEditMode = false,
          startDate = Some(DateModel("10", "10", "2020")),
          accountingMethod = Some(Cash)
        )

        Then("Should return a SEE_OTHER and redirect to check your answers page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.PropertyCheckYourAnswersController.show().url)
        )
      }
    }
  }
}

