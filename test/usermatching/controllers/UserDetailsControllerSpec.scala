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

package usermatching.controllers

import assets.MessageLookup.{UserDetails => messages}
import core.ITSASessionKeys
import core.auth.UserMatching
import core.config.MockConfig
import core.controllers.ControllerBaseSpec
import core.models.DateModel
import core.services.mocks.MockKeystoreService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, contentAsString, contentType, _}
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, SessionKeys}
import usermatching.forms.UserDetailsForm
import usermatching.models.UserDetailsModel
import usermatching.services.mocks.MockUserLockoutService
import core.utils.TestConstants._

class UserDetailsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockUserLockoutService {

  override val controllerName: String = "UserDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsController.show(isEditMode = false),
    "submit" -> TestUserDetailsController.submit(isEditMode = false)
  )

  def createTestUserDetailsController(enableMatchingFeature: Boolean) = new UserDetailsController(
    mockBaseControllerConfig(new MockConfig {
      override val userMatchingFeature = enableMatchingFeature
    }),
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockUserLockoutService
  )

  lazy val TestUserDetailsController = createTestUserDetailsController(enableMatchingFeature = true)

  lazy val request = userMatchingRequest.withSession(SessionKeys.userId -> testUserId.value, ITSASessionKeys.JourneyStateKey -> UserMatching.name)

  "When user matching is disabled" should {
    lazy val TestUserDetailsController: UserDetailsController = createTestUserDetailsController(enableMatchingFeature = false)

    "show" should {
      "return NOT FOUND" in {
        val result = TestUserDetailsController.show(isEditMode = false)(request)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for user matching is not yet available to the public:")
      }
    }

    "submit" should {
      "return NOT FOUND" in {
        val result = TestUserDetailsController.submit(isEditMode = false)(request)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for user matching is not yet available to the public:")
      }
    }
  }

  "When user matching is disabled" should {

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

        "Calling the submit action of the UserDetailsController with an authorised user and valid submission and" when {

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

            s"redirect to '${usermatching.controllers.routes.ConfirmUserController.show().url}" in {
              setupMockKeystore(
                fetchUserDetails = None,
                deleteAll = HttpResponse(OK)
              )
              setupMockNotLockedOut(testUserId)

              val goodResult = callSubmit(isEditMode = editMode)

              status(goodResult) must be(Status.SEE_OTHER)
              redirectLocation(goodResult) mustBe Some(usermatching.controllers.routes.ConfirmUserController.show().url)

              await(goodResult)
              verifyKeystore(fetchUserDetails = 1, saveUserDetails = 1, deleteAll = 0)
            }

          }

          "stored user details is different to the new user details" should {

            s"redirect to '${usermatching.controllers.routes.ConfirmUserController.show().url} and deleted all pre-existing entries in keystore" in {
              setupMockKeystore(
                fetchUserDetails = testUserDetails.copy(firstName = testUserDetails.firstName + "NOT"),
                deleteAll = HttpResponse(OK)
              )
              setupMockNotLockedOut(testUserId)

              val goodResult = callSubmit(isEditMode = editMode)

              status(goodResult) must be(Status.SEE_OTHER)
              redirectLocation(goodResult) mustBe Some(usermatching.controllers.routes.ConfirmUserController.show().url)

              await(goodResult)
              verifyKeystore(fetchUserDetails = 1, saveUserDetails = 1, deleteAll = 1)
            }

          }

          "stored user details is the same as the new user details" should {

            s"redirect to '${usermatching.controllers.routes.ConfirmUserController.show().url} but do not delete keystore" in {
              setupMockKeystore(
                fetchUserDetails = testUserDetails,
                deleteAll = HttpResponse(OK)
              )
              setupMockNotLockedOut(testUserId)

              val goodResult = callSubmit(isEditMode = editMode)

              status(goodResult) must be(Status.SEE_OTHER)
              redirectLocation(goodResult) mustBe Some(usermatching.controllers.routes.ConfirmUserController.show().url)

              await(goodResult)
              verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0, deleteAll = 0)
            }

          }
        }

        "Calling the submit action of the UserDetailsController with an authorised user and invalid submission" should {

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

    "If the user is locked out" should {
      s"calling show should redirect them to ${usermatching.controllers.routes.UserDetailsLockoutController.show().url}" in {
        setupMockLockedOut(testUserId)
        lazy val result = TestUserDetailsController.show(isEditMode = false)(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsLockoutController.show().url
      }

      s"calling submit should redirect them to ${usermatching.controllers.routes.UserDetailsLockoutController.show().url}" in {
        setupMockLockedOut(testUserId)
        lazy val result = TestUserDetailsController.submit(isEditMode = false)(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsLockoutController.show().url
      }
    }

  }

  authorisationTests()
}
