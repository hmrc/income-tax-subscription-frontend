/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent.matching

import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockClientDetailsJourneyRefiner, MockIdentifierAction}
import forms.agent.ClientDetailsForm
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, session, status, stubMessagesControllerComponents}
import services.mocks._
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.{dobD, dobM, dobY, firstName, lastName, nino}
import views.agent.matching.mocks.MockClientDetails

import scala.concurrent.Future

class ClientDetailsControllerSpec extends ControllerSpec
  with MockClientDetails
  with MockIdentifierAction
  with MockClientDetailsJourneyRefiner
  with MockUserLockoutService
  with MockAuditingService
  with MockSessionClearingService {

  implicit override val cc: MessagesControllerComponents =
    stubMessagesControllerComponents()

  implicit val messages: Messages =
    cc.messagesApi.preferred(request)

  "show" must {
    "return OK with the page content" when {
      "the user is not locked out and is not in edit mode" in {
        setupMockNotLockedOut(testARN)
        mockView(
          postAction = routes.ClientDetailsController.submit(),
          isEditMode = false
        )

        val result: Future[Result] = TestClientDetailsController.show(false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is not locked out and is in edit mode" in {
        setupMockNotLockedOut(testARN)
        mockView(
          postAction = routes.ClientDetailsController.submit(editMode = true),
          isEditMode = true
        )

        val result: Future[Result] = TestClientDetailsController.show(true)(request.withSession(
          firstName -> "FirstName",
          lastName -> "LastName",
          dobD -> "1",
          dobM -> "2",
          dobY -> "1980",
          nino -> "ZZ111111Z"
        ))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "redirect to the client details lockout page" when {
      "the lockout service indicates the user is locked out" in {
        setupMockLockedOut(testARN)

        val result: Future[Result] = TestClientDetailsController.show(false)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ClientDetailsLockoutController.show.url)
      }
    }
    "throw an InternalServerException" when {
      "there is a failure from the lockout service" in {
        setupMockLockStatusFailureResponse(testARN)

        intercept[InternalServerException](await(TestClientDetailsController.show(false)(request)))
          .message mustBe "[ClientDetailsController][handleLockOut] lockout failure"
      }
    }
  }

  "submit" must {
    "redirect to the confirm client page adding the client details to session" when {
      "the user is not locked out, is not in edit mode and has no validation errors" in {
        setupMockNotLockedOut(testARN)
        mockClearAgentSessionSuccess(Redirect(routes.ConfirmClientController.show()))

        val result: Future[Result] = TestClientDetailsController.submit(false)(
          request.withMethod("POST").withFormUrlEncodedBody(
            ClientDetailsForm.clientFirstName -> "FirstName",
            ClientDetailsForm.clientLastName -> "LastName",
            s"${ClientDetailsForm.clientDateOfBirth}-dateDay" -> "1",
            s"${ClientDetailsForm.clientDateOfBirth}-dateMonth" -> "2",
            s"${ClientDetailsForm.clientDateOfBirth}-dateYear" -> "1980",
            ClientDetailsForm.clientNino -> "AA000011D"
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmClientController.show().url)

        val sessionMap: Map[String, String] = session(result).data

        sessionMap.get(firstName) mustBe Some("FirstName")
        sessionMap.get(lastName) mustBe Some("LastName")
        sessionMap.get(dobD) mustBe Some("1")
        sessionMap.get(dobM) mustBe Some("2")
        sessionMap.get(dobY) mustBe Some("1980")
        sessionMap.get(nino) mustBe Some("AA000011D")
      }
    }
    "return a bad request with the page content" which {
      "is not in edit mode" when {
        "the input produces an error and is not in edit mode" in {
          setupMockNotLockedOut(testARN)
          mockView(
            postAction = routes.ClientDetailsController.submit(),
            isEditMode = false
          )

          val result: Future[Result] = TestClientDetailsController.submit(false)(
            request.withMethod("POST").withFormUrlEncodedBody()
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
      }
      "is in edit mode" when {
        "the input produces an error and is in edit mode" in {
          setupMockNotLockedOut(testARN)
          mockView(
            postAction = routes.ClientDetailsController.submit(editMode = true),
            isEditMode = true
          )

          val result: Future[Result] = TestClientDetailsController.submit(true)(
            request.withMethod("POST").withFormUrlEncodedBody()
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
      }
    }
    "redirect to the client details lockout page" when {
      "the lockout service indicates the user is locked out" in {
        setupMockLockedOut(testARN)

        val result: Future[Result] = TestClientDetailsController.submit(false)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ClientDetailsLockoutController.show.url)
      }
    }
    "throw an InternalServerException" when {
      "there is a failure from the lockout service" in {
        setupMockLockStatusFailureResponse(testARN)

        intercept[InternalServerException](await(TestClientDetailsController.submit(false)(request)))
          .message mustBe "[ClientDetailsController][handleLockOut] lockout failure"
      }
    }
  }

  object TestClientDetailsController extends ClientDetailsController (
    mockClientDetails,
    fakeIdentifierAction,
    fakeClientDetailsJourneyRefiner,
    mockUserLockoutService,
    mockAuditingService,
    mockSessionClearingService
  )

}