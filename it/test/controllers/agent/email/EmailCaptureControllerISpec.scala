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
import forms.agent.email.EmailCaptureForm
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.libs.ws.WSResponse

class EmailCaptureControllerISpec extends ComponentSpecBase {

  s"GET ${routes.EmailCaptureController.show().url}" when {
    "the user is not authorised" must {
      "redirect the use to the login page" in {
        AuthStub.stubUnauthorised()

        val result = showEmailCapture()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/email-capture"))
        )
      }
    }
    "the user does not have a journey state" must {
      "redirect to cannot go back to previous client" in {
        AuthStub.stubAuthSuccess()

        val result = showEmailCapture(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }

    "the user is authorised and in a confirmed client state" must {
      "return OK with the page content" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))

        val result = showEmailCapture()

        result must have(
          httpStatus(OK),
          pageTitle(s"${messages("agent.email-capture.heading")} - $serviceNameGovUk")
        )
      }
    }
  }

  s"POST ${routes.EmailCaptureController.submit().url}" when {
    "the user is not authorised" must {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = submitEmailCapture(Some(testValidEmail))()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/email-capture"))
        )
      }
    }
    "the user does not have a journey state" must {
      "redirect to cannot go back to previous client" in {
        AuthStub.stubAuthSuccess()

        val result = submitEmailCapture(Some(testValidEmail))(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
    "the user is authorised and in a confirmed client state" must {
      "redirect to the What You Need To Do page" when {
        "a valid email is submitted" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          val result = submitEmailCapture(Some(testValidEmail))()

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          )
        }
      }
      "return a BAD_REQUEST" when {
        "the email address is missing" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          val result = submitEmailCapture(None)()

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("agent.email-capture.heading")} - $serviceNameGovUk"),
            errorDisplayed()
          )
        }
        "the email address is too long" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          val result = submitEmailCapture(Some("a" * 245 + "@email.com"))()

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("agent.email-capture.heading")} - $serviceNameGovUk"),
            errorDisplayed()
          )
        }
        "the email address is invalid" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.NINO -> JsString(testNino),
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))

          val result = submitEmailCapture(Some("a@b.c."))()

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("agent.email-capture.heading")} - $serviceNameGovUk"),
            errorDisplayed()
          )
        }
      }
    }
  }

  lazy val serviceNameGovUk = "Use software to report your client’s Income Tax - GOV.UK"
  lazy val testValidEmail = "test@email.com"

  def showEmailCapture(includeState: Boolean = true): WSResponse = {
    IncomeTaxSubscriptionFrontend.get("/email-capture", ClientData.basicClientData, withJourneyStateSignUp = includeState)
  }

  def submitEmailCapture(request: Option[String])(includeState: Boolean = true): WSResponse = {
    IncomeTaxSubscriptionFrontend.post("/email-capture", ClientData.basicClientData, withJourneyStateSignUp = includeState)(
      request.fold(Map.empty[String, Seq[String]])(
        model => EmailCaptureForm.form.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )
  }

}
