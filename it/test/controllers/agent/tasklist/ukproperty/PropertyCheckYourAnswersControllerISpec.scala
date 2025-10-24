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

package controllers.agent.tasklist.ukproperty

import common.Constants.ITSASessionKeys
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.subscriptionUri
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{AgentURI, testNino, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.WiremockHelper.verifyPost
import helpers.agent.servicemocks.AuthStub
import models.DateModel
import models.common.PropertyModel
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.Property

class PropertyCheckYourAnswersControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/uk-property-check-your-answers" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(startDateBeforeLimit = Some(false))))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

      When("GET business/uk-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.getPropertyCheckYourAnswers()

      Then("Should return OK with the property CYA page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("agent.property.check-your-answers.title")} - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
        )
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/uk-property-check-your-answers" when {
    "redirect to the your clients income sources page" when {
      "the client has answered all the questions for uk property" in {
        AuthStub.stubAuthSuccess()
        val testProperty = PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(DateModel("10", "11", "2021")))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(testProperty))
        IncomeTaxSubscriptionConnectorStub.stubSaveProperty(testProperty.copy(confirmed = true))
        IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of the your income sources page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.yourIncomeSourcesURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveProperty(
          PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(DateModel("10", "11", "2021")), confirmed = true),
          Some(1)
        )
      }

      "the client has answered partial questions for uk property" in {
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(startDateBeforeLimit = Some(false))))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When("POST business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.yourIncomeSourcesURI)
        )
        verifyPost(subscriptionUri(Property), count = Some(0))
      }
    }
  }

  "return INTERNAL_SERVER_ERROR" when {
    "the property details could not be retrieved" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

      When("POST business/uk-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

      Then("Should return a INTERNAL_SERVER_ERROR")
      res must have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "the property details could not be confirmed" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
        Property,
        OK,
        Json.toJson(PropertyModel(startDate = Some(DateModel("10", "11", "2021"))))
      )
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(Property)
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

      When("POST business/uk-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

      Then("Should return a INTERNAL_SERVER_ERROR")
      res must have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }
  }

}
