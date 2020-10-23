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

package controllers.usermatching

import assets.MessageLookup.{UserDetails => messages}
import auth.individual.UserMatching
import controllers.ControllerBaseSpec
import utilities.individual.TestConstants._
import forms.usermatching.UserDetailsForm
import models.DateModel
import models.usermatching.UserDetailsModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, contentType, _}
import services.mocks.{MockSubscriptionDetailsService, MockUserLockoutService}
import uk.gov.hmrc.http.{HttpResponse, SessionKeys}
import utilities.ITSASessionKeys

import scala.concurrent.Future

class UserDetailsControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockUserLockoutService {

  override val controllerName: String = "UserDetailsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsController.show(isEditMode = false),
    "submit" -> TestUserDetailsController.submit(isEditMode = false)
  )

  object TestUserDetailsController extends UserDetailsController(
    mockAuthService,
    MockSubscriptionDetailsService,
    mockUserLockoutService
  )

  val testUserDetails =
    UserDetailsModel(
      firstName = "Abc",
      lastName = "Abc",
      nino = testNino,
      dateOfBirth = DateModel("01", "01", "1980")
    )

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = userMatchingRequest.withSession(
    SessionKeys.userId -> testCredId, ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "Calling the show action of the UserDetailsController with an authorised user" should {
    def call(request: Request[AnyContent]): Future[Result] = TestUserDetailsController.show(isEditMode = false)(request)

    "return ok (200)" in {
      setupMockNotLockedOut(testCredId)

      val r = request.buildRequest(None)

      val result = call(r)

      status(result) must be(Status.OK)
      await(result).verifyStoredUserDetailsIs(None)(r)

      verifySubscriptionDetailsDeleteAll(0)

      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))

      val document = Jsoup.parse(contentAsString(result))
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      document.title mustBe messages.title + serviceNameGovUk
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
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK))
            setupMockNotLockedOut(testCredId)

            val r = request.buildRequest(None)

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.usermatching.routes.ConfirmUserController.show().url)

            // the submitted details is now stored in session
            await(goodResult).verifyStoredUserDetailsIs(testUserDetails)(r)
            verifySubscriptionDetailsDeleteAll(0)
          }

        }

        "stored user details is different to the new user details" should {

          s"redirect to '${controllers.usermatching.routes.ConfirmUserController.show().url} and deleted all pre-existing entries in Subscription Details " in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK))
            setupMockNotLockedOut(testCredId)

            val previousUserDetails = testUserDetails.copy(firstName = testUserDetails.firstName + "NOT")

            val r = request.buildRequest(previousUserDetails)

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.usermatching.routes.ConfirmUserController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(testUserDetails)(r)
            verifySubscriptionDetailsDeleteAll(1)
          }

        }

        "stored user details is the same as the new user details" should {

          s"redirect to '${controllers.usermatching.routes.ConfirmUserController.show().url} but do not delete Subscription Details " in {
            mockDeleteAllFromSubscriptionDetails(HttpResponse(OK))
            setupMockNotLockedOut(testCredId)

            val r = request.buildRequest(testUserDetails)

            val goodResult = callSubmit(r, isEditMode = editMode)

            status(goodResult) must be(Status.SEE_OTHER)
            redirectLocation(goodResult) mustBe Some(controllers.usermatching.routes.ConfirmUserController.show().url)

            await(goodResult).verifyStoredUserDetailsIs(testUserDetails)(r)
            verifySubscriptionDetailsDeleteAll(0)
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

        def callSubmit(isEditMode: Boolean): Future[Result] =
          TestUserDetailsController.submit(isEditMode = isEditMode)(testRequest)

        "return a redirect status (BAD_REQUEST - 400)" in {
          setupMockSubscriptionDetailsSaveFunctions()
          setupMockNotLockedOut(testCredId)

          val badResult = callSubmit(isEditMode = editMode)

          status(badResult) must be(Status.BAD_REQUEST)

          await(badResult).verifyStoredUserDetailsIs(None)(testRequest)
          verifySubscriptionDetailsDeleteAll(0)
        }

        "return HTML" in {
          setupMockNotLockedOut(testCredId)

          val badResult = callSubmit(isEditMode = editMode)

          contentType(badResult) must be(Some("text/html"))
          charset(badResult) must be(Some("utf-8"))
        }

        "render the 'User Details page'" in {
          setupMockNotLockedOut(testCredId)

          val badResult = callSubmit(isEditMode = editMode)
          val document = Jsoup.parse(contentAsString(badResult))
          val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
          document.title mustBe "Error: " + messages.title + serviceNameGovUk
        }

      }
    }

  }

  "If the user is locked out" should {
    s"calling show should redirect them to ${controllers.usermatching.routes.UserDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testCredId)
      lazy val result = TestUserDetailsController.show(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.usermatching.routes.UserDetailsLockoutController.show().url
    }

    s"calling submit should redirect them to ${controllers.usermatching.routes.UserDetailsLockoutController.show().url}" in {
      setupMockLockedOut(testCredId)
      lazy val result = TestUserDetailsController.submit(isEditMode = false)(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.usermatching.routes.UserDetailsLockoutController.show().url
    }
  }


  authorisationTests()
}
