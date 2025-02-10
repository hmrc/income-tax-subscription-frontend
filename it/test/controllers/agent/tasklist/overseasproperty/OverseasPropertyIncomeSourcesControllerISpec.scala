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

package controllers.agent.tasklist.overseasproperty

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import forms.agent.IncomeSourcesOverseasPropertyForm
import helpers.IntegrationTestConstants.testUtr
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsString, Json}
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.OverseasProperty
import utilities.agent.TestConstants.testNino


class OverseasPropertyIncomeSourcesControllerISpec extends ComponentSpecBase {

  s"GET ${routes.IncomeSourcesOverseasPropertyController.show().url}" when {
    "the Subscription Details Connector returns no data" should {
      "show the overseas property income sources page with empty form" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the overseas property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.foreign-property.income-sources.title") + serviceNameGovUk),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.startDateBeforeLimit, None),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.accountingMethodOverseasProperty, None)
        )
      }
    }

    "the Subscription Details Connector returns start date before limit only" should {
      "show the overseas property income sources page with start date before limit pre-filled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(startDateBeforeLimit = Some(true))))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the overseas property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.foreign-property.income-sources.title") + serviceNameGovUk),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.startDateBeforeLimit, Some("Yes")),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.accountingMethodOverseasProperty, None)
        )
      }
    }

    "the Subscription Details Connector returns accounting method only" should {
      "show the property income sources page with accounting method pre-filled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the overseas property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.foreign-property.income-sources.title") + serviceNameGovUk),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.startDateBeforeLimit, None),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.accountingMethodOverseasProperty, Some("Cash basis accounting"))
        )
      }
    }

    "the Subscription Details Connector returns full data" should {
      "show the overseas property income sources page with full data pre-filled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(
          startDateBeforeLimit = Some(true),
          accountingMethod = Some(Cash)
        )))

        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("GET /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyIncomeSources()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the overseas property income sources page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.foreign-property.income-sources.title") + serviceNameGovUk),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.startDateBeforeLimit, Some("Yes")),
          radioButtonSet(IncomeSourcesOverseasPropertyForm.accountingMethodOverseasProperty, Some("Cash basis accounting"))
        )
      }
    }
  }

  s"POST ${routes.IncomeSourcesOverseasPropertyController.submit().url}" when {
    "return BAD_REQUEST" when {
      "nothing is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /business/income-sources-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyIncomeSources(
          isEditMode = false,
          maybeOverseasProperty = None
        )

        Then("Should return a BAD_REQUEST and show the overseas property income sources page with errors")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.foreign-property.income-sources.title") + " - Use software to report your client’s Income Tax - GOV.UK")
        )
      }

      "only start date before limit is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyIncomeSources(
          isEditMode = false,

          maybeOverseasProperty = Some(OverseasPropertyModel(
            startDateBeforeLimit = Some(true)
          ))
        )

        Then("Should return a BAD_REQUEST and show the property income sources page with errors")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.foreign-property.income-sources.title") + " - Use software to report your client’s Income Tax - GOV.UK")
        )
      }

      "only accounting method is submitted" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyIncomeSources(
          isEditMode = false,

          maybeOverseasProperty = Some(OverseasPropertyModel(
            accountingMethod = Some(Cash)
          ))
        )

        Then("Should return a BAD_REQUEST and show the overseas property income sources page with errors")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.foreign-property.income-sources.title") + " - Use software to report your client’s Income Tax - GOV.UK")
        )
      }
    }

    "redirect to check your answers page" when {
      "full data is submitted with the users start date is before the limit" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(OverseasPropertyModel(
          accountingMethod = Some(Cash),
          startDateBeforeLimit = Some(true)
        ))
        IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

        When("POST /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyIncomeSources(
          isEditMode = false,

          maybeOverseasProperty = Some(OverseasPropertyModel(
            startDateBeforeLimit = Some(true),
            accountingMethod = Some(Cash)
          ))

        )

        Then("Should return a SEE_OTHER and redirect to check your answers page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.OverseasPropertyCheckYourAnswersController.show().url)
        )
      }
    }

    "redirect to the overseas property start date page" when {
      "full data is submitted with the users start date is not before the limit" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(fullOverseasPropertyModel.copy(startDate = None))
        IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

        When("POST /business/income-sources-overseas-property is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyIncomeSources(
          isEditMode = false,
          maybeOverseasProperty = Some(fullOverseasPropertyModel.copy(startDate = None))
        )

        Then("Should return a SEE_OTHER and redirect to check your answers page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.OverseasPropertyStartDateController.show().url)
        )
      }
    }
  }

  lazy val startDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  lazy val fullOverseasPropertyModel: OverseasPropertyModel = OverseasPropertyModel(
    startDateBeforeLimit = Some(false),
    accountingMethod = Some(Cash),
    startDate = Some(startDate)
  )

}