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

import _root_.common.Constants.ITSASessionKeys
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.subscriptionUri
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants._
import helpers.agent.ComponentSpecBase
import helpers.agent.WiremockHelper.verifyPost
import helpers.agent.servicemocks.AuthStub
import models.DateModel
import models.common.OverseasPropertyModel
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.OverseasProperty
import utilities.UserMatchingSessionUtil

class OverseasPropertyCheckYourAnswersControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-check-your-answers" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel()))
      SessionDataConnectorStub.stubGetAllSessionData(Map(
        ITSASessionKeys.NINO -> JsString(testNino),
        ITSASessionKeys.UTR -> JsString(testUtr)
      ))

      When("GET business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers(
        Map(
          UserMatchingSessionUtil.firstName -> testFirstName,
          UserMatchingSessionUtil.lastName -> testLastName
        )
      )

      Then("Should return OK with the property CYA page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("agent.overseas-property.check-your-answers.title")} - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
        )
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-check-your-answers" when {
    "redirect to your clients income sources page" when {
      "the client has answered all the questiosn for overseas property" in {
        val testProperty = OverseasPropertyModel(
          startDate = Some(DateModel("10", "11", "2021"))
        )
        val expectedProperty = testProperty.copy(confirmed = true)

        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testProperty))
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expectedProperty)
        IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(
          Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          )
        )

        Then("Should return a SEE_OTHER with a redirect location of the your income sources page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.yourIncomeSourcesURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expectedProperty, Some(1))
      }

      "the client has answered partial questions for overseas property" in {
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel()))
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(
          Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          )
        )

        Then("Should return a SEE_OTHER with a redirect location of the your income sources page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(AgentURI.yourIncomeSourcesURI)
        )

        verifyPost(subscriptionUri(OverseasProperty), count = Some(0))
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "overseas property details could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(
          Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          )
        )

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "overseas property details cannot be confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK,
          Json.toJson(OverseasPropertyModel(startDate = Some(DateModel("10", "11", "2021")))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(OverseasProperty)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(
          Map(
            UserMatchingSessionUtil.firstName -> testFirstName,
            UserMatchingSessionUtil.lastName -> testLastName
          )
        )

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
