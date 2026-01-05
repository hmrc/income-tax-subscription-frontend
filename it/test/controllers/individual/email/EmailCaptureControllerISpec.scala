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

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.servicemocks.AuthStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}

class EmailCaptureControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = "Sign up for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.EmailCaptureController.show().url}" when {
    "the user is not authorised" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.showEmailCapture()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }
    "the user does not have any state" must {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showEmailCapture(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.matching.routes.HomeController.index.url)
        )
      }
    }
    "the user is authorised and in the sign up state" must {
      "return the page with content" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.showEmailCapture()

        result must have(
          httpStatus(OK),
          pageTitle(s"${messages("individual.email-capture.heading")} - $serviceNameGovUk")
        )
      }
    }
  }

  s"POST ${routes.EmailCaptureController.submit().url}" when {
    "the user is not authorised" must {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitEmailCapture(None)()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }
    "the user does not have any state" must {
      "redirect to home" in {
        AuthStub.stubAuthSuccess()

        val result = IncomeTaxSubscriptionFrontend.submitEmailCapture(None)(includeState = false)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.matching.routes.HomeController.index.url)
        )
      }
    }
    "the user is authorised and in the sign up state" must {
      "return a BAD_REQUEST with the page content" when {
        "the email address is missing" in {
          AuthStub.stubAuthSuccess()

          val result = IncomeTaxSubscriptionFrontend.submitEmailCapture(Some(""))()

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("individual.email-capture.heading")} - $serviceNameGovUk"),
            errorDisplayed()
          )
        }
        "the email address is too long" in {
          AuthStub.stubAuthSuccess()

          val result = IncomeTaxSubscriptionFrontend.submitEmailCapture(Some("a@b.c" * 51))()

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("individual.email-capture.heading")} - $serviceNameGovUk"),
            errorDisplayed()
          )
        }
        "the email address is invalid" in {
          AuthStub.stubAuthSuccess()

          val result = IncomeTaxSubscriptionFrontend.submitEmailCapture(Some("a@b.c."))()

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("individual.email-capture.heading")} - $serviceNameGovUk"),

            errorDisplayed()
          )
        }
      }
      "redirect to the ORM page" when {
        "a valid email address is provided" in {
          AuthStub.stubAuthSuccess()

          val result = IncomeTaxSubscriptionFrontend.submitEmailCapture(Some("a@b.c"))()

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.routes.WhatYouNeedToDoController.show.url)
          )
        }
      }
    }
  }

}
