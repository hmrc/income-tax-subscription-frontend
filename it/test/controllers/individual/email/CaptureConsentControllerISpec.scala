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

package controllers.individual.email

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.servicemocks.AuthStub
import models.{No, Yes, YesNo}
import play.api.http.Status._
import play.api.libs.json.JsString
import play.api.libs.ws.WSResponse

class CaptureConsentControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = "Use software to send Income Tax updates - GOV.UK"

  s"GET ${routes.CaptureConsentController.show().url}" when {
    "the user is not authorised" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.showCaptureConsent()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/capture-consent"))
        )
      }
    }
    "the user does not have any state" must {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showCaptureConsent(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.matching.routes.HomeController.index.url)
        )
      }
    }
    "the user is authorised and in the sign up state" must {
      "return the page with content" when {
        "the user previously selected yes" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.CAPTURE_CONSENT -> JsString(Yes.toString)
          ))

          val result = IncomeTaxSubscriptionFrontend.showCaptureConsent()

          result must have(
            httpStatus(OK),
            pageTitle(s"${messages("individual.capture-consent.heading")} - $serviceNameGovUk"),
            radioButtonSet(id = "yes-no", selectedRadioButton = Some(Yes.toString))
          )
        }
        "the user previously selected no" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.CAPTURE_CONSENT -> JsString(No.toString)
          ))
          val result = IncomeTaxSubscriptionFrontend.showCaptureConsent()

          result must have(
            httpStatus(OK),
            pageTitle(s"${messages("individual.capture-consent.heading")} - $serviceNameGovUk"),
            radioButtonSet(id = "yes-no", selectedRadioButton = Some(No.toString))
          )
        }
      }
    }
  }

  s"POST ${routes.CaptureConsentController.show().url}" when {
    "the user is not authorised" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()
        SessionDataConnectorStub.stubGetAllSessionData(Map())
        val result = IncomeTaxSubscriptionFrontend.submitCaptureConsent(None)()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/capture-consent"))
        )
      }
    }
    "the user does not have any state" must {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map())
        val result = IncomeTaxSubscriptionFrontend.submitCaptureConsent(None)(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.matching.routes.HomeController.index.url)
        )
      }
    }
    "the user is authorised and in the sign up state" must {
      s"return a redirect to ${controllers.individual.email.routes.EmailCaptureController.show().url}" when {
        "the user selects the Yes radio button" in {
          val userInput: YesNo = Yes
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.CAPTURE_CONSENT, userInput)(OK)

          When(s"POST ${controllers.individual.email.routes.CaptureConsentController.submit().url} is called")
          val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCaptureConsent(request = Some(userInput))()

          Then("Should return SEE_OTHER to the What You Need To Do controller")

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.email.routes.EmailCaptureController.show().url)
          )
        }
      }

      s"return a redirect to ${controllers.individual.routes.WhatYouNeedToDoController.show.url}" when {
        "the user selects the No radio button" in {
          val userInput: YesNo = No
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.CAPTURE_CONSENT, userInput)(OK)

          When(s"POST ${controllers.individual.email.routes.CaptureConsentController.submit().url} is called")
          val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCaptureConsent(request = Some(userInput))()

          Then("Should return SEE_OTHER to the What You Need To Do Controller")

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.routes.WhatYouNeedToDoController.show.url)
          )
        }
      }

      "return BAD_REQUEST and display an error box on screen without redirecting" when {

        "the user does not select either option" in {

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When(s"POST ${controllers.individual.email.routes.CaptureConsentController.submit().url} is called")
          val result: WSResponse = IncomeTaxSubscriptionFrontend.submitCaptureConsent(request = None)()

          Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("individual.capture-consent.heading")} - $serviceNameGovUk"),
            errorDisplayed(),
            elementTextBySelector(".govuk-error-message")(s"Error: ${messages("individual.capture-consent.form-error")}")
          )

        }
      }

      "return INTERNAL_SERVER_ERROR" when {

        "the Capture Consent Status could not be saved" in {
          val userInput: YesNo = Yes
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.CAPTURE_CONSENT, userInput)(INTERNAL_SERVER_ERROR)
          When(s"POST ${controllers.individual.email.routes.CaptureConsentController.submit().url} is called")
          val result = IncomeTaxSubscriptionFrontend.submitCaptureConsent(request = Some(userInput))()

          Then("Should return a INTERNAL_SERVER_ERROR")
          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

}
