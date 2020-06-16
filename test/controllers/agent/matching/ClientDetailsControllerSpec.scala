/*
 * Copyright 2020 HM Revenue & Customs
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

import agent.assets.MessageLookup.{ClientDetails => messages}
import controllers.agent.AgentControllerBaseSpec
import forms.agent.ClientDetailsForm
import models.DateModel
import models.usermatching.UserDetailsModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, contentType, _}
import services.mocks.MockKeystoreService
import services.mocks.MockUserLockoutService
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}
import utilities.agent.TestConstants

import scala.concurrent.Future


class ClientDetailsControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with MockUserLockoutService {

  override val controllerName: String = "ClientDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestClientDetailsController.show(isEditMode = false),
    "submit" -> TestClientDetailsController.submit(isEditMode = false)
  )

  object TestClientDetailsController extends ClientDetailsController(
    mockAuthService,
    MockKeystoreService,
    mockUserLockoutService
  )

  val testNino: String = TestConstants.testNino
  val testARN: String = TestConstants.testARN

  "Calling the show action of the ClientDetailsController with an authorised user" should {

    def call(request: Request[AnyContent]): Future[Result] = TestClientDetailsController.show(isEditMode = false)(request)

    "return ok (200) when not locked out" in {
      lazy val r = userMatchingRequest.buildRequest(None)
      setupMockNotLockedOut(testARN)
      lazy val result = call(r)

      status(result) must be(Status.OK)

      await(result).verifyStoredUserDetailsIs(None)(r)

      withClue("return HTML") {
        contentType(result) must be(Some("text/html"))
        charset(result) must be(Some("utf-8"))
      }

      withClue("render the 'Not subscribed to Agent Services page'") {
        val document = Jsoup.parse(contentAsString(result))
        document.title mustBe messages.title
      }
    }

    "return see_other (303) when locked out" in {
      lazy val r = userMatchingRequest.buildRequest(None)
      setupMockLockedOut(testARN)
      lazy val result = call(r)

      status(result) must be(Status.SEE_OTHER)

      await(result).verifyStoredUserDetailsIs(None)(r)

      withClue(s"redirect to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show().url}") {
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsLockoutController.show().url)
      }
    }

    "throw an internal server exception when a lockout error occurs" in {
      lazy val r = userMatchingRequest.buildRequest(None)
      setupMockLockStatusFailureResponse(testARN)
      lazy val result = await(call(r))

      intercept[InternalServerException](result).getMessage mustBe "[ClientDetailsController][handleLockOut] lockout failure"
    }

  }


  for (editMode <- Seq(true, false)) {

    s"when editMode=$editMode and" when {

      "Calling the submit action of the ClientDetailsController with an authorised user and valid submission and" when {

        val testClientDetails =
          UserDetailsModel(
            firstName = "Abc",
            lastName = "Abc",
            nino = testNino,
            dateOfBirth = DateModel("01", "01", "1980")
          )

        def callSubmit(request: FakeRequest[AnyContentAsEmpty.type])(isEditMode: Boolean): Future[Result] =
          TestClientDetailsController.submit(isEditMode = isEditMode)(
            request.post(ClientDetailsForm.clientDetailsForm.form, testClientDetails)
          )

        "there are no stored data" should {

          s"redirect to '${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
            mockDeleteAllFromKeyStore(HttpResponse(OK))
            setupMockNotLockedOut(testARN)

            lazy val r = userMatchingRequest.buildRequest(None)

            val goodResult = callSubmit(r)(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(testClientDetails)(r)
            verifyKeyStoreDeleteAll(0)
          }

        }

        "stored user details is different to the new user details" should {

          s"redirect to '${controllers.agent.matching.routes.ConfirmClientController.show().url} and deleted all pre-existing entries in keystore" in {
            mockDeleteAllFromKeyStore(HttpResponse(OK))
            setupMockNotLockedOut(testARN)

            val newUserDetails = testClientDetails.copy(firstName = testClientDetails.firstName + "NOT")

            lazy val r = userMatchingRequest.buildRequest(newUserDetails)

            val goodResult = callSubmit(r)(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(testClientDetails)(r)
            verifyKeyStoreDeleteAll(1)
          }

        }

        "stored user details is the same as the new user details" should {

          s"redirect to '${controllers.agent.matching.routes.ConfirmClientController.show().url} but do not delete keystore" in {
            mockDeleteAllFromKeyStore(HttpResponse(OK))
            setupMockNotLockedOut(testARN)

            lazy val r = userMatchingRequest.buildRequest(testClientDetails)

            val goodResult = callSubmit(r)(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(testClientDetails)(r)
            verifyKeyStoreDeleteAll(0)
          }

        }
      }

      "Calling the submit action of the ClientDetailsController with an authorised user and invalid submission" should {

        val newTestUserDetails = UserDetailsModel(
          firstName = "Abc",
          lastName = "Abc",
          nino = testNino,
          dateOfBirth = DateModel("00", "01", "1980"))

        def callSubmit(isEditMode: Boolean): Future[Result] =
          TestClientDetailsController.submit(isEditMode = isEditMode)(
            userMatchingRequest
              .post(ClientDetailsForm.clientDetailsForm.form, newTestUserDetails)
          )

        "return a redirect status (BAD_REQUEST - 400)" in {
          setupMockKeystoreSaveFunctions()
          setupMockNotLockedOut(testARN)

          val badResult = callSubmit(isEditMode = editMode)

          status(badResult) must be(Status.BAD_REQUEST)

          // bad requests do not trigger a save
          await(badResult).verifyStoredUserDetailsIs(None)(userMatchingRequest)
          verifyKeyStoreDeleteAll(0)
        }

        "return HTML" in {
          setupMockNotLockedOut(testARN)

          val badResult = callSubmit(isEditMode = editMode)

          contentType(badResult) must be(Some("text/html"))
          charset(badResult) must be(Some("utf-8"))
        }

        "render the 'Client Details page'" in {
          setupMockNotLockedOut(testARN)

          val badResult = callSubmit(isEditMode = editMode)
          val document = Jsoup.parse(contentAsString(badResult))
          document.title mustBe "Error: " + messages.title
        }

      }
    }

  }

  "If the agent is locked out" should {
    s"calling show should redirect them to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testARN)
      lazy val result = TestClientDetailsController.show(isEditMode = false)(userMatchingRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show().url
    }

    s"calling submit should redirect them to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testARN)
      lazy val result = TestClientDetailsController.submit(isEditMode = false)(userMatchingRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show().url
    }
  }

  authorisationTests()
}
