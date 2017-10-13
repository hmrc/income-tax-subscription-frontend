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

import auth.UserMatched
import controllers.{ControllerBaseSpec, ITSASessionKeys}
import org.scalatest.OptionValues
import play.api.http.Status
import play.api.mvc._
import play.api.test.Helpers.{await, _}
import services.mocks._
import uk.gov.hmrc.http.{HttpResponse, InternalServerException, SessionKeys}
import usermatching.services.mocks.{MockUserLockoutService, MockUserMatchingService}
import utils.TestConstants._
import utils.{TestConstants, TestModels}

import scala.concurrent.Future

class ConfirmUserControllerSpec extends ControllerBaseSpec
  with MockUserLockoutService with MockUserMatchingService with MockKeystoreService
  with OptionValues {

  override val controllerName: String = "ConfirmUserController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmUserController.show(),
    "submit" -> TestConfirmUserController.submit()
  )

  object TestConfirmUserController extends ConfirmUserController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockUserMatchingService,
    mockUserLockoutService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val userDetails = TestModels.testUserDetails
  val token = TestConstants.testToken

  lazy val request = userMatchingRequest.withSession(SessionKeys.userId -> testUserId.value)

  "Calling the show action of the ConfirmUserController with an authorised user" should {

    def call = TestConfirmUserController.show()(request)

    "when there are no user details store redirect them to user details" in {
      setupMockKeystore(fetchUserDetails = None)
      setupMockNotLockedOut(testUserId)

      val result = call

      status(result) must be(Status.SEE_OTHER)

      await(result)
      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)

    }

    "if there is are user details return ok (200)" in {
      setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
      setupMockNotLockedOut(testUserId)

      val result = call

      status(result) must be(Status.OK)

      await(result)

      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
    }
  }

  "Calling the submit action of the confirmUserController with a locked out user" should {
    def callSubmit(): Future[Result] = TestConfirmUserController.submit()(request)

    "return the user details page" in {
      setupMockLockedOut(testUserId)

      val result = callSubmit()

      redirectLocation(result) must contain(usermatching.controllers.routes.UserDetailsLockoutController.show().url)
    }
  }

  "Calling the submit action of the confirmUserController with no keystore data" should {
    def callSubmit(): Future[Result] = TestConfirmUserController.submit()(request)

    "return the user details page" in {
      setupMockKeystore(fetchUserDetails = None)
      setupMockNotLockedOut(testUserId)

      val result = callSubmit()

      redirectLocation(result) must contain(usermatching.controllers.routes.UserDetailsController.show().url)
    }
  }

  "Calling the submit action of the ConfirmUserController with an authorised user and valid submission" when {

    def callSubmit(): Future[Result] = TestConfirmUserController.submit()(request)

    "UserMatchingService returned UnexpectedFailure" should {
      "return a InternalServerException" in {
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
        mockUserMatchException(userDetails)
        setupMockNotLockedOut(testUserId)

        val result = callSubmit()

        intercept[InternalServerException](await(result))
      }
    }

    "UserMatchingService returns user with nino and utr" should {
      s"redirect to the home controller with nino and sautr added to session" in {
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
        mockUserMatchSuccess(userDetails)
        setupMockNotLockedOut(testUserId)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.HomeController.index().url)

        val session = await(result).session(request)
        session.get(ITSASessionKeys.NINO) must contain(TestConstants.testNino)
        session.get(ITSASessionKeys.UTR) must contain(TestConstants.testUtr)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(UserMatched.name)
      }
    }

    "UserMatchingService returns user with only nino" should {
      s"redirect to the home controller with nino added to session" in {
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
        mockUserMatchSuccessNoUtr(userDetails)
        setupMockNotLockedOut(testUserId)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.HomeController.index().url)

        val session = await(result).session(request)
        session.get(ITSASessionKeys.NINO) must contain(TestConstants.testNino)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(UserMatched.name)
      }
    }

    "UserMatchingService returns nothing" when {
      "the lockout count is 0" should {
        "redirect to the user details page and increment the counter by 1" in {
          setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
          mockUserMatchFailure(userDetails)
          setupMockNotLockedOut(testUserId)

          val result = callSubmit()

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(usermatching.controllers.routes.UserDetailsErrorController.show().url)

          val session = await(result).session(request)
          session.get(ITSASessionKeys.FailedUserMatching) must contain("1")
        }
      }

      "the lockout count is less than the maximum" should {
        "redirect to the user details page and increment the counter by 1" in {
          implicit val requestWithLockout = request.withSession(
            SessionKeys.userId -> testUserId.value,
            ITSASessionKeys.FailedUserMatching -> "1"
          )

          setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
          mockUserMatchFailure(userDetails)
          setupMockNotLockedOut(testUserId)

          val result = TestConfirmUserController.submit()(requestWithLockout)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(usermatching.controllers.routes.UserDetailsErrorController.show().url)

          val session = await(result).session
          session.get(ITSASessionKeys.FailedUserMatching) must contain("2")
        }
      }

      "the lockout count reaches the maximum" should {
        "lockout the user and redirect to the locked out page" in {
          implicit val requestWithLockout = request.withSession(
            SessionKeys.userId -> testUserId.value,
            ITSASessionKeys.FailedUserMatching -> "3"
          )

          setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
          mockUserMatchFailure(userDetails)
          setupMockNotLockedOut(testUserId)
          setupMockLockCreated(testUserId)
          setupMockKeystore(deleteAll = HttpResponse(Status.OK))

          val result = TestConfirmUserController.submit()(requestWithLockout)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(usermatching.controllers.routes.UserDetailsLockoutController.show().url)

          val session = await(result).session
          session.get(ITSASessionKeys.FailedUserMatching) mustBe empty

          verifyLockoutUser(testUserId, 1)
        }
      }
    }
  }
}
