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
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService, MockUserLockoutService}
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}
import utilities.agent.TestConstants
import views.html.agent.ClientDetails

import scala.concurrent.Future


class ClientDetailsControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockUserLockoutService
  with MockAuditingService {

  override val controllerName: String = "ClientDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
  )

  private def withController(testCode: ClientDetailsController => Any): Unit = {

    val clientDetailsView = app.injector.instanceOf[ClientDetails]

    val controller = new ClientDetailsController(
      mockAuditingService,
      mockAuthService,
      mockUserLockoutService,
      clientDetailsView

    )
    testCode(controller)
  }

  val testNino: String = TestConstants.testNino
  val testARN: String = TestConstants.testARN

  "Calling the show action of the ClientDetailsController with an authorised user" should withController { controller =>

    def call(request: Request[AnyContent]): Future[Result] = controller.show(isEditMode = false)(request)

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

      withClue("render the 'Enter your client’s details' page") {
        val document = Jsoup.parse(contentAsString(result))
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        val title = document.getElementsByTag("title").text()

        title mustBe messages.title + serviceNameGovUk
      }
    }

    "return see_other (303) when locked out" in {
      lazy val r = userMatchingRequest.buildRequest(None)
      setupMockLockedOut(testARN)
      lazy val result = call(r)

      status(result) must be(Status.SEE_OTHER)

      await(result).verifyStoredUserDetailsIs(None)(r)

      withClue(s"redirect to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show.url}") {
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsLockoutController.show.url)
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

      "Calling the submit action of the ClientDetailsController with an authorised user and valid submission and" when withController { controller =>

        val testClientDetails =
          UserDetailsModel(
            firstName = "Abc",
            lastName = "Abc",
            nino = testNino,
            dateOfBirth = DateModel("1", "1", "1980")
          )

        def callSubmit(request: FakeRequest[AnyContentAsEmpty.type])(isEditMode: Boolean): Future[Result] =
          controller.submit(isEditMode = isEditMode)(
            request.post(ClientDetailsForm.clientDetailsForm, testClientDetails)
          )

        "there are no stored data" should {

          s"redirect to '${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK, ""))
            setupMockNotLockedOut(testARN)

            lazy val r = userMatchingRequest.buildRequest(None)

            val goodResult = callSubmit(r)(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(Some(testClientDetails))(r)
          }

        }

        "stored user details is different to the new user details" should {

          s"redirect to '${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK, ""))
            setupMockNotLockedOut(testARN)

            val newUserDetails = testClientDetails.copy(firstName = testClientDetails.firstName + "NOT")

            lazy val r = userMatchingRequest.buildRequest(Some(newUserDetails))

            val goodResult = callSubmit(r)(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(Some(testClientDetails))(r)
          }

        }

        "stored user details is the same as the new user details" should {

          s"redirect to '${controllers.agent.matching.routes.ConfirmClientController.show().url} but do not delete Subscription Details " in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK, ""))
            setupMockNotLockedOut(testARN)

            lazy val r = userMatchingRequest.buildRequest(Some(testClientDetails))

            val goodResult = callSubmit(r)(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(Some(testClientDetails))(r)
          }

        }
      }

      "Calling the submit action of the ClientDetailsController with an authorised user and invalid submission" should withController { controller =>

        val newTestUserDetails = UserDetailsModel(
          firstName = "Abc",
          lastName = "Abc",
          nino = testNino,
          dateOfBirth = DateModel("00", "01", "1980"))

        def callSubmit(isEditMode: Boolean): Future[Result] =
          controller.submit(isEditMode = isEditMode)(
            userMatchingRequest
              .post(ClientDetailsForm.clientDetailsForm, newTestUserDetails)
          )

        "return a bad request status (BAD_REQUEST - 400)" in {
          setupMockSubscriptionDetailsSaveFunctions()
          setupMockNotLockedOut(testARN)

          val badResult = callSubmit(isEditMode = editMode)

          status(badResult) must be(Status.BAD_REQUEST)

          // bad requests do not trigger a save
          await(badResult).verifyStoredUserDetailsIs(None)(userMatchingRequest)
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
          val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
          document.getElementsByTag("title").text mustBe "Error: " + messages.title + serviceNameGovUk
        }

      }
    }

  }

  "If the agent is locked out" should withController { controller =>
    s"calling show should redirect them to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show.url}" in {
      setupMockLockedOut(testARN)
      lazy val result = controller.show(isEditMode = false)(userMatchingRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show.url
    }

    s"calling submit should redirect them to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show.url}" in {
      setupMockLockedOut(testARN)
      lazy val result = controller.submit(isEditMode = false)(userMatchingRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show.url
    }
  }

  authorisationTests()
}
