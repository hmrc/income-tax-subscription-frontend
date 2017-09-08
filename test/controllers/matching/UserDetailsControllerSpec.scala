/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.matching

import assets.MessageLookup.{UserDetails => messages}
import controllers.{ControllerBaseSpec, ITSASessionKeys}
import forms.UserDetailsForm
import models.DateModel
import models.matching.UserDetailsModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, contentAsString, contentType, _}
import services.mocks.{MockKeystoreService, MockUserLockoutService}
import uk.gov.hmrc.play.http.{HttpResponse, SessionKeys}
import utils.TestConstants._
import utils.TestModels._


class UserDetailsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockUserLockoutService {

  override val controllerName: String = "ClientDetailsController"
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
  lazy val request = fakeRequest.withSession(SessionKeys.userId -> testUserId.value, ITSASessionKeys.GoHome -> "et")

  "Calling the show action of the UserDetailsController with an authorised user" should {
    lazy val result = await(TestUserDetailsController.show(isEditMode = false)(request))

    "return ok (200)" in {
      setupMockKeystore(fetchUserDetails = None)
      setupMockNotLockedOut(testUserId)

      status(result) must be(Status.OK)

      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0, deleteAll = 0)

      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))

      val document = Jsoup.parse(contentAsString(result))
      document.title mustBe messages.title
    }
  }


  for (editMode <- Seq(true, false)) {

    s"when editMode=$editMode and" when {

      "Calling the submit action of the ClientDetailsController with an authorised user and valid submission and" when {

        val testUserDetails =
          UserDetailsModel(
            firstName = "Abc",
            lastName = "Abc",
            nino = testNino,
            dateOfBirth = DateModel("01", "01", "1980")
          )

        def callSubmit(isEditMode: Boolean) =
          TestUserDetailsController.submit(isEditMode = isEditMode)(
            request.post(UserDetailsForm.userDetailsForm.form, testUserDetails)
          )

        "there are no stored data" should {

          s"redirect to '${controllers.matching.routes.ConfirmUserController.show().url}" in {
            setupMockKeystore(
              fetchUserDetails = None,
              deleteAll = HttpResponse(OK)
            )
            setupMockNotLockedOut(testUserId)

            val goodResult = callSubmit(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.matching.routes.ConfirmUserController.show().url)

            await(goodResult)
            verifyKeystore(fetchUserDetails = 1, saveUserDetails = 1, deleteAll = 0)
          }

        }

        "stored user details is different to the new user details" should {

          s"redirect to '${controllers.matching.routes.ConfirmUserController.show().url} and deleted all pre-existing entries in keystore" in {
            setupMockKeystore(
              fetchUserDetails = testUserDetails.copy(firstName = testUserDetails.firstName + "NOT"),
              deleteAll = HttpResponse(OK)
            )
            setupMockNotLockedOut(testUserId)

            val goodResult = callSubmit(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.matching.routes.ConfirmUserController.show().url)

            await(goodResult)
            verifyKeystore(fetchUserDetails = 1, saveUserDetails = 1, deleteAll = 1)
          }

        }

        "stored user details is the same as the new user details" should {

          s"redirect to '${controllers.matching.routes.ConfirmUserController.show().url} but do not delete keystore" in {
            setupMockKeystore(
              fetchUserDetails = testUserDetails,
              deleteAll = HttpResponse(OK)
            )
            setupMockNotLockedOut(testUserId)

            val goodResult = callSubmit(isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.matching.routes.ConfirmUserController.show().url)

            await(goodResult)
            verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0, deleteAll = 0)
          }

        }
      }

      "Calling the submit action of the ClientDetailsController with an authorised user and invalid submission" should {

        def callSubmit(isEditMode: Boolean) =
          TestUserDetailsController.submit(isEditMode = isEditMode)(
            request
              .post(UserDetailsForm.userDetailsForm.form, UserDetailsModel(
                firstName = "Abc",
                lastName = "Abc",
                nino = testNino,
                dateOfBirth = DateModel("00", "01", "1980")))
          )

        "return a redirect status (BAD_REQUEST - 400)" in {
          setupMockKeystoreSaveFunctions()
          setupMockNotLockedOut(testUserId)

          val badResult = callSubmit(isEditMode = editMode)

          status(badResult) must be(Status.BAD_REQUEST)

          await(badResult)
          verifyKeystore(fetchUserDetails = 0, saveUserDetails = 0, deleteAll = 0)
        }

        "return HTML" in {
          setupMockNotLockedOut(testUserId)

          val badResult = callSubmit(isEditMode = editMode)

          contentType(badResult) must be(Some("text/html"))
          charset(badResult) must be(Some("utf-8"))
        }

        "render the 'Not subscribed to Agent Services page'" in {
          setupMockNotLockedOut(testUserId)

          val badResult = callSubmit(isEditMode = editMode)
          val document = Jsoup.parse(contentAsString(badResult))
          document.title mustBe messages.title
        }

      }
    }

  }

  "If the agent is locked out" should {
    s"calling show should redirect them to ${controllers.matching.routes.UserDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testUserId)
      lazy val result = TestUserDetailsController.show(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.matching.routes.UserDetailsLockoutController.show().url
    }

    s"calling submit should redirect them to ${controllers.matching.routes.UserDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testUserId)
      lazy val result = TestUserDetailsController.submit(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.matching.routes.UserDetailsLockoutController.show().url
    }
  }

  authorisationTests()
}
