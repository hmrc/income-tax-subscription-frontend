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

package controllers.usermatching

import agent.audit.mocks.MockAuditingService
import auth.individual.UserMatching
import controllers.ControllerBaseSpec
import forms.usermatching.UserDetailsForm
import models.DateModel
import models.usermatching.UserDetailsModel
import play.api.http.Status
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentType, _}
import services.mocks.{MockSubscriptionDetailsService, MockUserLockoutService}
import uk.gov.hmrc.http.HttpResponse
import utilities.ITSASessionKeys
import utilities.individual.TestConstants._
import views.individual.usermatching.mocks.MockUserDetails

import scala.concurrent.Future

class UserDetailsControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockUserLockoutService
  with MockAuditingService
  with MockUserDetails {

  override val controllerName: String = "UserDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsController.show(isEditMode = false),
    "submit" -> TestUserDetailsController.submit(isEditMode = false)
  )

  object TestUserDetailsController extends UserDetailsController(
    mockAuditingService,
    mockAuthService,
    userDetails,
    mockUserLockoutService
  )

  val testUserDetails: UserDetailsModel =
    UserDetailsModel(
      firstName = "Abc",
      lastName = "Abc",
      nino = testNino,
      dateOfBirth = DateModel("01", "01", "1980")
    )

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = userMatchingRequest.withSession(
    ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "Calling the show action of the UserDetailsController with an authorised user" should {
    def call(request: Request[AnyContent]): Future[Result] = TestUserDetailsController.show(isEditMode = false)(request)

    "return ok (200)" in {
      setupMockNotLockedOut(testCredId)
      mockUserDetails()

      val r = request.buildRequest(None)

      val result = call(r)

      status(result) must be(Status.OK)
      await(result).verifyStoredUserDetailsIs(None)(r)

      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }
  }


  for (editMode <- Seq(true, false)) {

    s"when editMode=$editMode and" when {

      "Calling the submit action of the UserDetailsController with an authorised user and valid submission and" when {

        def callSubmit(request: FakeRequest[_], isEditMode: Boolean): Future[Result] = {
          TestUserDetailsController.submit(isEditMode = isEditMode)(
            request.post(UserDetailsForm.userDetailsForm.form, testUserDetails)
          )
        }

        "there are no stored data" should {

          s"redirect to '${controllers.usermatching.routes.ConfirmUserController.show().url}" in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK, ""))
            setupMockNotLockedOut(testCredId)
            mockUserDetails()

            val r = request.buildRequest(None)

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.usermatching.routes.ConfirmUserController.show().url)

            // the submitted details is now stored in session
            await(goodResult).verifyStoredUserDetailsIs(Some(testUserDetails))(r)
          }

        }

        "stored user details is different to the new user details" should {

          s"redirect to '${controllers.usermatching.routes.ConfirmUserController.show().url} and deleted all pre-existing entries in Subscription Details " in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK, ""))
            setupMockNotLockedOut(testCredId)
            mockUserDetails()

            val previousUserDetails = testUserDetails.copy(firstName = testUserDetails.firstName + "NOT")

            val r = request.buildRequest(Some(previousUserDetails))

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.usermatching.routes.ConfirmUserController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(Some(testUserDetails))(r)
          }

        }

        "stored user details is the same as the new user details" should {

          s"redirect to '${controllers.usermatching.routes.ConfirmUserController.show().url} but do not delete Subscription Details " in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK, ""))
            setupMockNotLockedOut(testCredId)

            val r = request.buildRequest(Some(testUserDetails))

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.usermatching.routes.ConfirmUserController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(Some(testUserDetails))(r)
          }

        }
      }

      "Calling the submit action of the UserDetailsController with an authorised user and invalid submission" should {
        mockUserDetails()

        lazy val testRequest = request
          .post(UserDetailsForm.userDetailsForm.form, UserDetailsModel(
            firstName = "Abc",
            lastName = "Abc",
            nino = testNino,
            dateOfBirth = DateModel("00", "01", "1980")))

        def callSubmit(isEditMode: Boolean): Future[Result] =
          TestUserDetailsController.submit(isEditMode = isEditMode)(testRequest)

        "return a redirect status (BAD_REQUEST - 400)" in {
          setupMockSubscriptionDetailsSaveFunctions()
          setupMockNotLockedOut(testCredId)
          mockUserDetails()

          val badResult = callSubmit(isEditMode = editMode)

          status(badResult) must be(Status.BAD_REQUEST)

          await(badResult).verifyStoredUserDetailsIs(None)(testRequest)
        }

        "return HTML" in {
          setupMockNotLockedOut(testCredId)
          mockUserDetails()

          val badResult = callSubmit(isEditMode = editMode)

          contentType(badResult) must be(Some("text/html"))
          charset(badResult) must be(Some("utf-8"))
        }

      }
    }

  }

  "If the user is locked out" should {
    s"calling show should redirect them to ${controllers.usermatching.routes.UserDetailsLockoutController.show.url}" in {
      setupMockLockedOut(testCredId)
      mockUserDetails()
      lazy val result = TestUserDetailsController.show(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.usermatching.routes.UserDetailsLockoutController.show.url
    }

    s"calling submit should redirect them to ${controllers.usermatching.routes.UserDetailsLockoutController.show.url}" in {
      setupMockLockedOut(testCredId)
      mockUserDetails()
      lazy val result = TestUserDetailsController.submit(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.usermatching.routes.UserDetailsLockoutController.show.url
    }
  }


  authorisationTests()
}
