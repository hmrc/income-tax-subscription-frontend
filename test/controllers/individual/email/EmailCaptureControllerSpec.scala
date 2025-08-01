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

import common.Constants.ITSASessionKeys.FULLNAME
import controllers.ControllerSpec
import controllers.individual.actions.mocks.MockSignUpJourneyRefiner
import forms.individual.email.EmailCaptureForm
import models.audits.BetaContactDetails
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockAuditingService
import views.individual.email.mocks.MockEmailCapture

import scala.concurrent.Future

class EmailCaptureControllerSpec extends ControllerSpec
  with MockSignUpJourneyRefiner
  with MockAuditingService
  with MockEmailCapture {

  "show" must {
    "return OK with the page content" in {
      mockView(
        form = EmailCaptureForm.form,
        postAction = routes.EmailCaptureController.submit(),
        backUrl = controllers.individual.email.routes.CaptureConsentController.show().url
      )

      val result: Future[Result] = TestEmailCaptureController.show(request)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" must {
    "redirect to the ORM page and audit the beta contact details" when {
      "the user enters a valid email address" when {
        "the user has their name in session" in {
          val result: Future[Result] = TestEmailCaptureController.submit(
            request.withSession(FULLNAME -> testFullName).withMethod("POST").withFormUrlEncodedBody(EmailCaptureForm.formKey -> testValidEmail)
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)

          verifyAudit(BetaContactDetails(
            emailAddress = testValidEmail,
            agentReferenceNumber = None,
            fullName = Some(testFullName),
            nino = Some(nino)
          ))
        }
        "the user does not have their name in session" in {
          val result: Future[Result] = TestEmailCaptureController.submit(
            request.withMethod("POST").withFormUrlEncodedBody(EmailCaptureForm.formKey -> testValidEmail)
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)

          verifyAudit(BetaContactDetails(
            emailAddress = testValidEmail,
            agentReferenceNumber = None,
            fullName = None,
            nino = Some(nino)
          ))
        }
      }
    }
    "return a bad request" when {
      "an invalid input was provided" in {
        mockView(
          form = EmailCaptureForm.form.bind(Map.empty[String, String]),
          postAction = routes.EmailCaptureController.submit(),
          backUrl = controllers.individual.email.routes.CaptureConsentController.show().url
        )

        val result: Future[Result] = TestEmailCaptureController.submit(
          request.withMethod("POST").withFormUrlEncodedBody()
        )

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  lazy val testValidEmail: String = "test@email.com"
  lazy val testFullName: String = "FirstName LastName"

  object TestEmailCaptureController extends EmailCaptureController(
    auditingService = mockAuditingService,
    identify = fakeIdentifierAction,
    journeyRefiner = fakeSignUpJourneyRefiner,
    view = mockEmailCapture
  )

}
