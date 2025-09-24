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

package controllers.agent.tasklist.addbusiness

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.AgentURI.globalCheckYourAnswersURI
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino, testUtr}
import helpers.IntegrationTestModels._
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status._
import play.api.libs.json.{JsBoolean, JsString, Json}
import play.api.libs.ws.WSResponse
import utilities.SubscriptionDataKeys

class YourIncomeSourceToSignUpControllerISpec extends ComponentSpecBase {

  private val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

  s"GET ${routes.YourIncomeSourceToSignUpController.show.url}" should {
    "return OK" when {
      "there are no income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, OK, JsBoolean(true))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSourcesAgent()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.your-income-source.heading") + serviceNameGovUk)
        )
      }
      "there are multiple income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, testBusinesses.getOrElse(Seq.empty))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSourcesAgent()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.your-income-source.heading") + serviceNameGovUk)
        )
      }
    }
    "return SEE_OTHER" when {
      "the user is not authenticated" in {
        AuthStub.stubUnauthorised()

        val result: WSResponse = IncomeTaxSubscriptionFrontend.yourIncomeSourcesAgent()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/your-income-source"))
        )
      }
    }
  }

  s"POST ${routes.YourIncomeSourceToSignUpController.submit.url}" when {
    "the user is not authenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitYourIncomeSourcesAgent()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/your-income-source"))
        )
      }
    }
    "the user has complete businesses" should {
      "redirect to the declaration page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
          OK, Seq(testBusiness("12345", confirmed = true))
        )
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true)

        When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSourcesAgent()

        Then("Should return a SEE_OTHER to the declaration page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(globalCheckYourAnswersURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(1))
      }
    }
    "the user has incomplete businesses" should {
      "redirect to the incomplete income sources page" when {
        "a sole trader business is incomplete" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
            OK, Seq(testBusiness("12345"))
          )
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

          val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSourcesAgent()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.routes.IncomeSourcesIncompleteController.show.url)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
        }
        "a uk property business is incomplete" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
            OK, Seq(testBusiness("12345", confirmed = true))
          )
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel.copy(confirmed = false)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

          val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSourcesAgent()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.routes.IncomeSourcesIncompleteController.show.url)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
        }
        "a foreign property business is incomplete" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
            OK, Seq(testBusiness("12345", confirmed = true))
          )
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel.copy(confirmed = false)))

          val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSourcesAgent()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.routes.IncomeSourcesIncompleteController.show.url)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
        }
      }
    }
  }

}
