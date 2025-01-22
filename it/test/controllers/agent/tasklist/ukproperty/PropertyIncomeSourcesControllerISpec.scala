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
import config.featureswitch.FeatureSwitch.StartDateBeforeLimit
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import forms.agent.UkPropertyIncomeSourcesForm
import helpers.IntegrationTestConstants.testUtr
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.common.PropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsString, Json}
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.Property
import utilities.agent.TestConstants.testNino


class PropertyIncomeSourcesControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(StartDateBeforeLimit)
  }

  s"GET ${routes.PropertyIncomeSourcesController.show().url}" when {
    "the start date before limit feature switch is enabled" when {
      "the Subscription Details Connector returns no data" should {
        "show the property income sources page with empty form" in {
          enable(StartDateBeforeLimit)

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
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            radioButtonSet(UkPropertyIncomeSourcesForm.startDateBeforeLimit, None),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, None)
          )
        }
      }

      "the Subscription Details Connector returns start date only" should {
        "show the property income sources page with start date pre-filled" in {
          enable(StartDateBeforeLimit)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(startDateBeforeLimit = Some(true))))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

          val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the property income sources page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            radioButtonSet(UkPropertyIncomeSourcesForm.startDateBeforeLimit, Some("Yes")),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, None)
          )
        }
      }

      "the Subscription Details Connector returns accounting method only" should {
        "show the property income sources page with accounting method pre-filled" in {
          enable(StartDateBeforeLimit)

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
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            radioButtonSet(UkPropertyIncomeSourcesForm.startDateBeforeLimit, None),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, Some("Cash basis accounting"))
          )
        }
      }

      "the Subscription Details Connector returns full data" should {
        "show the property income sources page with full data pre-filled" in {
          enable(StartDateBeforeLimit)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(fullPropertyModel))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

          val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the property income sources page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            radioButtonSet(UkPropertyIncomeSourcesForm.startDateBeforeLimit, Some("No")),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, Some("Cash basis accounting"))
          )
        }
      }
    }
    "the start date before limit feature switch is disabled" when {
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
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            govukDateField(UkPropertyIncomeSourcesForm.startDate, DateModel("", "", "")),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, None)
          )
        }
      }

      "the Subscription Details Connector returns start date only" should {
        "show the property income sources page with start date pre-filled" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(startDate = Some(startDate))))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

          val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the property income sources page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            govukDateField(UkPropertyIncomeSourcesForm.startDate, startDate),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, None)
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
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            govukDateField(UkPropertyIncomeSourcesForm.startDate, DateModel("", "", "")),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, Some("Cash basis accounting"))
          )
        }
      }

      "the Subscription Details Connector returns full data" should {
        "show the property income sources page with full data pre-filled" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(fullPropertyModel))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("GET /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.ukPropertyIncomeSources()

          val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
          Then("Should return a OK with the property income sources page")
          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.property.income-source.heading") + serviceNameGovUk),
            govukDateField(UkPropertyIncomeSourcesForm.startDate, startDate),
            radioButtonSet(UkPropertyIncomeSourcesForm.accountingMethodProperty, Some("Cash basis accounting"))
          )
        }
      }
    }
  }

  s"POST ${routes.PropertyIncomeSourcesController.submit().url}" when {
    "the start date before limit feature switch is enabled" should {
      "return BAD_REQUEST" when {
        "nothing is submitted" in {
          enable(StartDateBeforeLimit)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("POST /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(isEditMode = false, maybeProperty = None)

          Then("Should return a BAD_REQUEST and show the property income sources page with errors")
          res must have(
            httpStatus(BAD_REQUEST),
            pageTitle("Error: " + messages("agent.property.income-source.heading") + " - Use software to report your client’s Income Tax - GOV.UK")
          )
        }

        "only start date before limit is submitted" in {
          enable(StartDateBeforeLimit)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("POST /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
            isEditMode = false,
            maybeProperty = Some(fullPropertyModel.copy(startDate = None, accountingMethod = None))
          )

          Then("Should return a BAD_REQUEST and show the property income sources page with errors")
          res must have(
            httpStatus(BAD_REQUEST),
            pageTitle("Error: " + messages("agent.property.income-source.heading") + " - Use software to report your client’s Income Tax - GOV.UK")
          )
        }

        "only accounting method is submitted" in {
          enable(StartDateBeforeLimit)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("POST /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
            isEditMode = false,
            maybeProperty = Some(fullPropertyModel.copy(startDateBeforeLimit = None, startDate = None))
          )

          Then("Should return a BAD_REQUEST and show the property income sources page with errors")
          res must have(
            httpStatus(BAD_REQUEST),
            pageTitle("Error: " + messages("agent.property.income-source.heading") + " - Use software to report your client’s Income Tax - GOV.UK")
          )
        }
      }

      "redirect to check your answers page" when {
        "full data is submitted with the users start date is before the limit" in {
          enable(StartDateBeforeLimit)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(fullPropertyModel.copy(startDate = None, startDateBeforeLimit = Some(true)))
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

          When("POST /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
            isEditMode = false,
            maybeProperty = Some(fullPropertyModel.copy(startDate = None, startDateBeforeLimit = Some(true)))
          )

          Then("Should return a SEE_OTHER and redirect to check your answers page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(routes.PropertyCheckYourAnswersController.show().url)
          )
        }
      }

      "redirect to the property start date page" when {
        "full data is submitted with the users start date is not before the limit" in {
          enable(StartDateBeforeLimit)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(fullPropertyModel.copy(startDate = None))
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

          When("POST /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
            isEditMode = false,
            maybeProperty = Some(fullPropertyModel.copy(startDate = None))
          )

          Then("Should return a SEE_OTHER and redirect to check your answers page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(routes.PropertyStartDateController.show().url)
          )
        }
      }
    }
    "the start date before limit feature switch is disabled" should {
      "return BAD_REQUEST" when {
        "nothing is submitted" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When("POST /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(isEditMode = false, maybeProperty = None)

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
            maybeProperty = Some(fullPropertyModel.copy(startDateBeforeLimit = None, accountingMethod = None))
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
            maybeProperty = Some(fullPropertyModel.copy(startDateBeforeLimit = None, startDate = None))
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
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(fullPropertyModel.copy(startDateBeforeLimit = None))
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

          When("POST /business/income-sources-property is called")
          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyIncomeSources(
            isEditMode = false,
            maybeProperty = Some(fullPropertyModel.copy(startDateBeforeLimit = None))
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

  lazy val startDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  lazy val fullPropertyModel: PropertyModel = PropertyModel(
    startDateBeforeLimit = Some(false),
    accountingMethod = Some(Cash),
    startDate = Some(startDate)
  )

}

