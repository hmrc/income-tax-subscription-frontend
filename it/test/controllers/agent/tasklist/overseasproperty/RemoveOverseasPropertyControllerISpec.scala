/*
 * Copyright 2022 HM Revenue & Customs
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
import helpers.IntegrationTestConstants.{AgentURI, basGatewaySignIn}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys.{IncomeSourceConfirmation, OverseasProperty}
import utilities.agent.TestConstants.{testNino, testUtr}

class RemoveOverseasPropertyControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/remove-overseas-property-business" should {
    "redirect to the login page" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.getRemoveClientOverseasProperty()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/business/remove-overseas-property-business"))
        )
      }
    }

    "return OK when foreign property exists" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK,
        Json.toJson(OverseasPropertyModel()))

      When("GET client/business/remove-overseas-property-business is called")
      val res = IncomeTaxSubscriptionFrontend.getRemoveClientOverseasProperty()
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      Then("Should return a OK with the client remove Overseas property confirmation page displaying")
      res must have(
        httpStatus(OK),
        pageTitle(messages("agent.remove-overseas-property-business.heading") + serviceNameGovUk)
      )
    }

    "redirect to Business Already Removed page when foreign property no longer exists" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT,
        Json.toJson(OverseasPropertyModel(None)))

      When("GET client/business/remove-uk-property-business is called")
      val res = IncomeTaxSubscriptionFrontend.getRemoveClientOverseasProperty()

      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show().url)
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/remove-overseas-property-business" should {
    "redirect to the your income sources page" when {
      "the user submits the 'yes' answer" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(OverseasProperty)
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)

        When("POST client/business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("Yes")))

        Then("Should return a SEE_OTHER with a redirect location of client task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.yourIncomeSourcesURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(1))
        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation, Some(1))
      }

      "the user submits the 'no' answer" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST client/business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("No")))

        Then("Should return a SEE_OTHER with a redirect location of client task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.yourIncomeSourcesURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
      }
    }

    "return a BAD_REQUEST" when {
      "no option was selected on the client remove Overseas property page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST /business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("")))

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res must have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "failed to delete foreign property" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetailsFailure(OverseasProperty)

        When("POST /business/remove-uk-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("Yes")))

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "failed to delete income source confirmation " in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(OverseasProperty)
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetailsFailure(IncomeSourceConfirmation)


        When("POST /business/remove-uk-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("Yes")))

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}

