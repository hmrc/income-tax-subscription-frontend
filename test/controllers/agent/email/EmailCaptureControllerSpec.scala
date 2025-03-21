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

import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.email.EmailCaptureForm
import models.audits.BetaContactDetails
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockAuditingService
import views.agent.email.mocks.MockEmailCapture

import scala.concurrent.Future

class EmailCaptureControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockAuditingService
  with MockEmailCapture {

  "show" must {
    "return OK" when {
      "the user is authorised and in a confirmed client state" in {
        mockView(form = EmailCaptureForm.form)

        val result: Future[Result] = TestEmailCaptureController.show(request = request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "redirect to the What You Need to Do page and audit the beta contact details" when {
      "the user enters a valid email address" in {
        val result: Future[Result] = TestEmailCaptureController.submit(
          request.withMethod("POST").withFormUrlEncodedBody(EmailCaptureForm.formKey -> testValidEmail)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)

        verifyAudit(BetaContactDetails(
          emailAddress = testValidEmail,
          agentReferenceNumber = Some(testARN),
          fullName = None,
          nino = None
        ))
      }
    }
    "return a bad request" when {
      "an invalid email is submitted" in {
        mockView(form = EmailCaptureForm.form.bind(Map.empty[String, String]))

        val result = TestEmailCaptureController.submit(request.withMethod("POST").withFormUrlEncodedBody())

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  lazy val testValidEmail: String = "test@email.com"

  object TestEmailCaptureController extends EmailCaptureController(
    identify = fakeIdentifierAction,
    journeyRefiner = fakeConfirmedClientJourneyRefiner,
    auditingService = mockAuditingService,
    view = mockEmailCapture
  )

}
