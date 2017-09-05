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

import controllers.{ControllerBaseSpec, ITSASessionKeys}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.{HeaderNames, Status}
import play.api.mvc._
import play.api.test.Helpers.{await, _}
import services._
import services.mocks._
import uk.gov.hmrc.play.http.{HttpResponse, InternalServerException, SessionKeys}
import utils.{SessionCookieBaker, TestConstants, TestModels}

import scala.concurrent.Future

class ConfirmUserControllerSpec extends ControllerBaseSpec
  with MockUserLockoutService with MockUserMatchingService with MockKeystoreService {

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

  lazy val sessionCookie = Session.encodeAsCookie(Session(Map(SessionKeys.token -> token, ITSASessionKeys.GoHome -> "et")))

  lazy val request =
    fakeRequest.withHeaders(play.api.http.HeaderNames.COOKIE -> Cookies.encodeSetCookieHeader(Seq(sessionCookie)))

  "Calling the show action of the ConfirmUserController with an authorised user" should {

    def call = TestConfirmUserController.show()(request)

    "when there are no client details store redirect them to client details" in {
      setupMockKeystore(fetchUserDetails = None)
      setupMockNotLockedOut(token)

      val result = call

      status(result) must be(Status.SEE_OTHER)

      await(result)
      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)

    }

    "if there is are client details return ok (200)" in {
      setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
      setupMockNotLockedOut(token)

      val result = call

      status(result) must be(Status.OK)

      await(result)

      verifyKeystore(fetchUserDetails = 1, saveUserDetails = 0)
    }
  }

  "Calling the submit action of the ConfirmUserController with an authorised user and valid submission" when {

    def callSubmit(): Future[Result] = TestConfirmUserController.submit()(request)

    "UserMatchingService returned UnexpectedFailure" should {
      "return a InternalServerException" in {
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
        mockUserMatchException(userDetails)
        setupMockNotLockedOut(token)

        val result = callSubmit()

        intercept[InternalServerException](await(result))
      }
    }

    "UserMatchingService returned NoUserDetails" should {
      s"redirect user to ${controllers.matching.routes.UserDetailsController.show().url}" in {
        setupMockKeystore(fetchUserDetails = TestModels.testUserDetails)
        mockUserMatchFailure(userDetails)
        setupMockNotLockedOut(token)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.matching.routes.UserDetailsController.show().url)
      }
    }
    //
    //    "UserMatchingService returned NoUserMatched" should {
    //      s"redirect user to ${controllers.matching.routes.UserDetailsErrorController.show().url}" in {
    //        mockOrchestrateAgentQualificationFailure(token, NoUserMatched)
    //        setupMockNotLockedOut(token)
    //
    //        val result = callSubmit()
    //
    //        status(result) mustBe SEE_OTHER
    //        redirectLocation(result) mustBe Some(controllers.matching.routes.UserDetailsErrorController.show().url)
    //      }
    //    }
    //
    //
    //    "UserMatchingService returned ApprovedAgent" should {
    //      s"redirect user to ${controllers.routes.IncomeSourceController.showIncomeSource().url}" in {
    //        mockOrchestrateAgentQualificationSuccess(token, nino)
    //        setupMockNotLockedOut(token)
    //
    //        val result = callSubmit()
    //
    //        status(result) mustBe SEE_OTHER
    //        redirectLocation(result) mustBe Some(controllers.routes.IncomeSourceController.showIncomeSource().url)
    //      }
    //    }
    //  }

    //  "An agent who is locked out" should {
    //    s"be redirect to ${controllers.matching.routes.UserDetailsLockoutController.show().url} when calling show" in {
    //      setupMockLockedOut(token)
    //
    //      val result = TestConfirmUserController.show()(request)
    //
    //      status(result) mustBe SEE_OTHER
    //
    //      redirectLocation(result).get mustBe controllers.matching.routes.UserDetailsLockoutController.show().url
    //    }
    //
    //    s"be redirect to ${controllers.matching.routes.UserDetailsLockoutController.show().url} when calling submit" in {
    //      setupMockLockedOut(token)
    //
    //      val result = TestConfirmUserController.submit()(request)
    //
    //      status(result) mustBe SEE_OTHER
    //
    //      redirectLocation(result).get mustBe controllers.matching.routes.UserDetailsLockoutController.show().url
    //    }
    //  }
    //
    //  "An agent who is not yet locked out" when {
    //
    //    "they fail client matching for the first time" should {
    //      def callSubmit(): Future[Result] = TestConfirmUserController.submit()(request)
    //
    //      lazy val result = callSubmit()
    //
    //      s"have the ${ITSASessionKeys.FailedClientMatching} -> 1 added to session" in {
    //        mockOrchestrateAgentQualificationFailure(token, NoUserMatched)
    //        setupMockNotLockedOut(token)
    //
    //        await(result).session(request).get(ITSASessionKeys.FailedClientMatching) mustBe Some(1.toString)
    //      }
    //
    //      s"redirect to ${controllers.matching.routes.UserDetailsErrorController.show().url}" in {
    //        status(result) mustBe SEE_OTHER
    //        redirectLocation(result) mustBe Some(controllers.matching.routes.UserDetailsErrorController.show().url)
    //      }
    //    }
    //
    //    "The back url" should {
    //      s"point to ${controllers.matching.routes.UserDetailsController.show().url}" in {
    //        TestConfirmUserController.backUrl mustBe controllers.matching.routes.UserDetailsController.show().url
    //      }
    //    }
    //
    //    authorisationTests()
    //  }
  }
}
