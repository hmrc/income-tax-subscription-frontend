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

package controllers.agent

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import models.{EligibilityStatus, No, Yes, YesNo}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.WSResponse
import utilities.agent.TestConstants.testUtr

class UsingSoftwareControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${controllers.agent.routes.UsingSoftwareController.show().url}" when {

    "the user is unauthenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.showUsingSoftware()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/using-software"))
        )
      }
    }

    "the Session Details Connector returns some data for Has Software" should {

      "show(false) the Using Software page with a radio option selected" in {
        val testOption: YesNo = Yes
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.HAS_SOFTWARE)(OK, Json.toJson(testOption))


        When(s"GET ${controllers.agent.routes.UsingSoftwareController.show(false).url}")
        val result = IncomeTaxSubscriptionFrontend.showUsingSoftware()

        Then("The result should be OK with page content")
        result must have(
          httpStatus(OK),
          pageTitle(messages("agent.using-software.heading") + serviceNameGovUk),
          radioButtonSet(id = "yes-no", selectedRadioButton = Some(testOption.toString))
        )
      }
    }

    "the Session Details Connector returns no data for Has Software" should {

      "show the Using Software page without a radio option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.HAS_SOFTWARE)(NO_CONTENT)


        When(s"GET ${controllers.agent.routes.UsingSoftwareController.show().url}")
        val result = IncomeTaxSubscriptionFrontend.showUsingSoftware()

        Then("The result should be OK with page content")
        result must have(
          httpStatus(OK),
          pageTitle(messages("agent.using-software.heading") + serviceNameGovUk),
          radioButtonSet(id = "yes-no", selectedRadioButton = None),
          radioButtonSet(id = "yes-no-2", selectedRadioButton = None)
        )
      }
    }

    "the session Details Connector return an error" should {

      "return INTERNAL_SERVER_ERROR" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.HAS_SOFTWARE)(INTERNAL_SERVER_ERROR)


        When(s"GET ${controllers.agent.routes.UsingSoftwareController.show().url}")
        val result = IncomeTaxSubscriptionFrontend.showUsingSoftware()

        Then("Should return a INTERNAL_SERVER_ERROR")
        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"POST ${controllers.agent.routes.UsingSoftwareController.submit().url}" should {
    "return a redirect to the login page" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitUsingSoftware(request = Some(Yes))

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/using-software"))
        )
      }
    }

    s"return a redirect to ${controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url}" when {
      "the user selects the Yes radio button" in {
        val userInput = Yes
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubSaveSessionData[YesNo](ITSASessionKeys.HAS_SOFTWARE, userInput)(OK)

        When(s"POST ${controllers.agent.routes.UsingSoftwareController.submit().url} is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitUsingSoftware(request = Some(userInput))

        Then("Should return SEE_OTHER to the What Year To Sign Up Controller")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
        )
      }
    }

    s"return a redirect to ${controllers.agent.routes.NoSoftwareController.show(false).url}" when {
      "the user submit(false)s the No radio button" in {
        val userInput = No

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubSaveSessionData[YesNo](ITSASessionKeys.HAS_SOFTWARE, userInput)(OK)

        When(s"POST ${controllers.agent.routes.UsingSoftwareController.submit(false).url} is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitUsingSoftware(request = Some(userInput))

        Then("Should return SEE_OTHER to the no software page")

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.NoSoftwareController.show(false).url)
        )
      }
    }

    "return BAD_REQUEST and display an error box on screen without redirecting" when {
      "the user does not select either option" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        When(s"POST ${controllers.agent.routes.UsingSoftwareController.submit(false).url} is called")
        val result: WSResponse = IncomeTaxSubscriptionFrontend.submitUsingSoftware(request = None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        result must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.using-software.heading") + serviceNameGovUk),
          errorDisplayed()
        )
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the Software Status could not be saved" in {
        val userInput: YesNo = Yes
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Mandated)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.HAS_SOFTWARE, userInput)(INTERNAL_SERVER_ERROR)

        When(s"POST ${controllers.agent.routes.UsingSoftwareController.submit(false).url} is called")
        val result = IncomeTaxSubscriptionFrontend.submitUsingSoftware(request = Some(userInput))

        Then("Should return a INTERNAL_SERVER_ERROR")
        result must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}















