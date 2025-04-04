/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.agent.email

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import forms.agent.email.CaptureConsentForm
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.{No, Yes, YesNo}
import play.api.http.Status._
import play.api.libs.json.JsString
import play.api.libs.ws.WSResponse

class CaptureConsentControllerISpec extends ComponentSpecBase {

  s"GET ${routes.CaptureConsentController.show().url}" when {
    "the user is not authorised" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = showCaptureConsent()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/capture-consent"))
        )
      }
    }
    "the user does not have any state" must {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()

        val result = showCaptureConsent(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "the user is authorised and in the sign up state" must {
      "return the page with content" when {
        "the user previously selected yes" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.CAPTURE_CONSENT)(OK, JsString(Yes.toString))
          val result = showCaptureConsent()

          result must have(
            httpStatus(OK),
            pageTitle(s"${messages("agent.capture-consent.heading")} - $serviceNameGovUk"),
            radioButtonSet(id = "yes-no", selectedRadioButton = Some(Yes.toString)),
            backUrl(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
          )
        }
        "the user previously selected no" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.CAPTURE_CONSENT)(OK, JsString(No.toString))
          val result = showCaptureConsent()

          result must have(
            httpStatus(OK),
            pageTitle(s"${messages("agent.capture-consent.heading")} - $serviceNameGovUk"),
            radioButtonSet(id = "yes-no", selectedRadioButton = Some(No.toString)),
            backUrl(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
          )
        }
        "the user previously selected nothing" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.CAPTURE_CONSENT)(NO_CONTENT)
          val result = showCaptureConsent()

          result must have(
            httpStatus(OK),
            pageTitle(s"${messages("agent.capture-consent.heading")} - $serviceNameGovUk"),
            radioButtonSet(id = "yes-no", selectedRadioButton = None),
            backUrl(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
          )
        }
      }
    }
  }

  s"POST ${routes.CaptureConsentController.show().url}" when {
    "the user is not authorised" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.CAPTURE_CONSENT)(OK)
        val result = submitCaptureConsent(None)()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/capture-consent"))
        )
      }
    }
    "the user does not have any state" must {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.CAPTURE_CONSENT)(OK)
        val result = submitCaptureConsent(None)(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "the user is authorised and in the sign up state" must {
      s"return a redirect to ${controllers.agent.email.routes.EmailCaptureController.show().url}" when {
        "the user selects the Yes radio button" in {
          val userInput: YesNo = Yes
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.CAPTURE_CONSENT, userInput)(OK)

          When(s"POST ${controllers.agent.email.routes.CaptureConsentController.submit().url} is called")
          val result = submitCaptureConsent(request = Some(userInput))()

          Then("Should return SEE_OTHER to the What You Need To Do controller")

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.email.routes.EmailCaptureController.show().url)
          )
        }
      }

      s"return a redirect to ${controllers.agent.routes.WhatYouNeedToDoController.show().url}" when {
        "the user selects the No radio button" in {
          val userInput: YesNo = No
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.CAPTURE_CONSENT, userInput)(OK)

          When(s"POST ${controllers.agent.email.routes.CaptureConsentController.submit().url} is called")
          val result = submitCaptureConsent(request = Some(userInput))()

          Then("Should return SEE_OTHER to the What You Need To Do Controller")

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          )
        }
      }

      "return BAD_REQUEST and display an error box on screen without redirecting" when {

        "the user does not select either option" in {

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          When(s"POST ${controllers.agent.email.routes.CaptureConsentController.submit().url} is called")
          val result = submitCaptureConsent(request = None)()

          Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("agent.capture-consent.heading")} - $serviceNameGovUk"),
            errorDisplayed(),
            elementTextBySelector(".govuk-error-message")(s"Error: ${messages("agent.capture-consent.form-error")}"),
            backUrl(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
          )

        }
      }

      "return INTERNAL_SERVER_ERROR" when {

        "the Capture Consent Status could not be saved" in {
          val userInput: YesNo = Yes
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.CAPTURE_CONSENT, userInput)(INTERNAL_SERVER_ERROR)
          When(s"POST ${controllers.agent.email.routes.CaptureConsentController.submit().url} is called")
          val result = submitCaptureConsent(request = Some(userInput))()

          Then("Should return a INTERNAL_SERVER_ERROR")
          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

  lazy val serviceNameGovUk = "Use software to report your client’s Income Tax - GOV.UK"

  def showCaptureConsent(includeState: Boolean = true): WSResponse = {
    IncomeTaxSubscriptionFrontend.get("/capture-consent", ClientData.basicClientData, withJourneyStateSignUp = includeState)
  }

  def submitCaptureConsent(request: Option[YesNo])(includeState: Boolean = true): WSResponse = {
    IncomeTaxSubscriptionFrontend.post("/capture-consent", ClientData.basicClientData, withJourneyStateSignUp = includeState)(
      request.fold(Map.empty[String, Seq[String]])(
        model => CaptureConsentForm.captureConsentForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )
  }

}
