/*
 * Copyright 2018 HM Revenue & Customs
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

package usermatching.controllers

import assets.MessageLookup.{UserDetails => messages}
import core.ITSASessionKeys
import core.auth.UserMatching
import core.controllers.ControllerBaseSpec
import core.models.DateModel
import core.services.mocks.MockKeystoreService
import core.utils.TestConstants._
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, contentType, _}
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, SessionKeys}
import usermatching.forms.UserDetailsForm
import usermatching.models.UserDetailsModel
import usermatching.services.mocks.MockUserLockoutService

class UserDetailsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockUserLockoutService {

  override val controllerName: String = "UserDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsController.show(isEditMode = false),
    "submit" -> TestUserDetailsController.submit(isEditMode = false)
  )


  object TestUserDetailsController extends UserDetailsController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockUserLockoutService
  )

  val testUserDetails =
    UserDetailsModel(
      firstName = "Abc",
      lastName = "Abc",
      nino = testNino,
      dateOfBirth = DateModel("01", "01", "1980")
    )

  lazy val request = userMatchingRequest.withSession(SessionKeys.userId -> testUserId.value, ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "Calling the show action of the UserDetailsController with an authorised user" should {
    def call(request: Request[AnyContent]) = TestUserDetailsController.show(isEditMode = false)(request)

    "return ok (200)" in {
      setupMockNotLockedOut(testUserId.value)

      val r = request.buildRequest(None)

      val result = call(r)

      status(result) must be(Status.OK)
      await(result).verifyStoredUserDetailsIs(None)(r)

      verifyKeystore(deleteAll = 0)

      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))

      val document = Jsoup.parse(contentAsString(result))
      document.title mustBe messages.title
    }
  }


  for (editMode <- Seq(true, false)) {

    s"when editMode=$editMode and" when {

      "Calling the submit action of the UserDetailsController with an authorised user and valid submission and" when {

        def callSubmit(request: FakeRequest[_], isEditMode: Boolean) = {
          TestUserDetailsController.submit(isEditMode = isEditMode)(
            request.post(UserDetailsForm.userDetailsForm.form, testUserDetails)
          )
        }

        "there are no stored data" should {

          s"redirect to '${usermatching.controllers.routes.ConfirmUserController.show().url}" in {
            setupMockKeystore(
              deleteAll = HttpResponse(OK)
            )
            setupMockNotLockedOut(testUserId.value)

            val r = request.buildRequest(None)

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(usermatching.controllers.routes.ConfirmUserController.show().url)

            // the submitted details is now stored in session
            await(goodResult).verifyStoredUserDetailsIs(testUserDetails)(r)
            verifyKeystore(deleteAll = 0)
          }

        }

        "stored user details is different to the new user details" should {

          s"redirect to '${usermatching.controllers.routes.ConfirmUserController.show().url} and deleted all pre-existing entries in keystore" in {
            setupMockKeystore(
              deleteAll = HttpResponse(OK)
            )
            setupMockNotLockedOut(testUserId.value)

            val previousUserDetails = testUserDetails.copy(firstName = testUserDetails.firstName + "NOT")

            val r = request.buildRequest(previousUserDetails)

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(usermatching.controllers.routes.ConfirmUserController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(testUserDetails)(r)
            verifyKeystore(deleteAll = 1)
          }

        }

        "stored user details is the same as the new user details" should {

          s"redirect to '${usermatching.controllers.routes.ConfirmUserController.show().url} but do not delete keystore" in {
            setupMockKeystore(
              deleteAll = HttpResponse(OK)
            )
            setupMockNotLockedOut(testUserId.value)

            val r = request.buildRequest(testUserDetails)

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(usermatching.controllers.routes.ConfirmUserController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(testUserDetails)(r)
            verifyKeystore(deleteAll = 0)
          }

        }
      }

      "Calling the submit action of the UserDetailsController with an authorised user and invalid submission" should {

        lazy val testRequest = request
          .post(UserDetailsForm.userDetailsForm.form, UserDetailsModel(
            firstName = "Abc",
            lastName = "Abc",
            nino = testNino,
            dateOfBirth = DateModel("00", "01", "1980")))

        def callSubmit(isEditMode: Boolean) =
          TestUserDetailsController.submit(isEditMode = isEditMode)(testRequest)

        "return a redirect status (BAD_REQUEST - 400)" in {
          setupMockKeystoreSaveFunctions()
          setupMockNotLockedOut(testUserId.value)

          val badResult = callSubmit(isEditMode = editMode)

          status(badResult) must be(Status.BAD_REQUEST)

          await(badResult).verifyStoredUserDetailsIs(None)(testRequest)
          verifyKeystore(deleteAll = 0)
        }

        "return HTML" in {
          setupMockNotLockedOut(testUserId.value)

          val badResult = callSubmit(isEditMode = editMode)

          contentType(badResult) must be(Some("text/html"))
          charset(badResult) must be(Some("utf-8"))
        }

        "render the 'User Details page'" in {
          setupMockNotLockedOut(testUserId.value)

          val badResult = callSubmit(isEditMode = editMode)
          val document = Jsoup.parse(contentAsString(badResult))
          document.title mustBe "Error: " + messages.title
        }

      }
    }

  }

  "If the user is locked out" should {
    s"calling show should redirect them to ${usermatching.controllers.routes.UserDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testUserId.value)
      lazy val result = TestUserDetailsController.show(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsLockoutController.show().url
    }

    s"calling submit should redirect them to ${usermatching.controllers.routes.UserDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testUserId.value)
      lazy val result = TestUserDetailsController.submit(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsLockoutController.show().url
    }
  }


  authorisationTests()
}
