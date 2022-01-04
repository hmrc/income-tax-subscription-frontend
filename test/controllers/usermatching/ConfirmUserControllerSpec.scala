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
import auth.individual.{UserMatched, UserMatching}
import controllers.ControllerBaseSpec
import models.audits.EnterDetailsAuditing
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.usermatching.UserDetailsModel
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.http.Status
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import services.mocks.{MockSubscriptionDetailsService, MockUserLockoutService, MockUserMatchingService}
import uk.gov.hmrc.http.HttpResponse
import utilities.individual.TestConstants
import utilities.individual.TestConstants._
import utilities.{ITSASessionKeys, TestModels}
import views.individual.mocks.MockCheckYourUserDetails
import scala.concurrent.Future

class ConfirmUserControllerSpec extends ControllerBaseSpec
  with MockCheckYourUserDetails
  with MockUserLockoutService
  with MockUserMatchingService
  with MockSubscriptionDetailsService
  with MockAuditingService
  with OptionValues {

  override val controllerName: String = "ConfirmUserController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmUserController.show(),
    "submit" -> TestConfirmUserController.submit()
  )

  object TestConfirmUserController extends ConfirmUserController(
    mockAuditingService,
    mockAuthService,
    mockUserLockoutService,
    mockUserMatchingService,
    checkYourUserDetails
  )

  val userDetails: UserDetailsModel = TestModels.testUserDetails
  val token: String = TestConstants.testToken

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = userMatchingRequest.withSession(
    ITSASessionKeys.JourneyStateKey -> UserMatching.name).buildRequest(userDetails)

  "Calling the show action of the ConfirmUserController with an authorised user" should {

    def call(request: Request[AnyContent]): Future[Result] = TestConfirmUserController.show()(request)

    "when there are no user details stored redirect them to user details" in {
      setupMockNotLockedOut(testCredId)

      val r = userMatchingRequest.withSession(ITSASessionKeys.JourneyStateKey -> UserMatching.name)

      val result = call(r)

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result) must contain(controllers.usermatching.routes.UserDetailsController.show().url)

      await(result).verifyStoredUserDetailsIs(None)(userMatchingRequest)
    }

    "if there are user details return ok (200)" in {
      setupMockNotLockedOut(testCredId)

      mockCheckYourUserDetails()

      val r = request.buildRequest(userDetails)

      val result = call(r)

      status(result) must be(Status.OK)

      await(result).verifyStoredUserDetailsIs(userDetails)(r)
    }
  }

  "Calling the submit action of the confirmUserController with a locked out user" should {
    def callSubmit(): Future[Result] = TestConfirmUserController.submit()(request)

    "return the user details page" in {
      setupMockLockedOut(testCredId)

      val result = callSubmit()

      redirectLocation(result) must contain(controllers.usermatching.routes.UserDetailsLockoutController.show.url)
    }
  }

  "Calling the submit action of the confirmUserController with no Subscription Details  data" should {
    def callSubmit(request: Request[AnyContent]): Future[Result] = TestConfirmUserController.submit()(request)

    "return the user details page" in {
      setupMockNotLockedOut(testCredId)

      val r = userMatchingRequest.withSession(ITSASessionKeys.JourneyStateKey -> UserMatching.name)

      val result = callSubmit(r)

      redirectLocation(result) must contain(controllers.usermatching.routes.UserDetailsController.show().url)
    }
  }

  "Calling the submit action of the ConfirmUserController with an authorised user and valid submission" when {

    def callSubmit(request: Request[AnyContent]): Future[Result] = TestConfirmUserController.submit()(request)

    "UserMatchingService returns user with nino and utr" should {
      s"redirect to the home controller with nino and sautr added to session" in {
        mockUserMatchSuccess(userDetails)
        setupMockNotLockedOut(testCredId)

        val r = request.buildRequest(userDetails)

        val result = await(callSubmit(r))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.index.url)

        val session = result.session(request)
        session.get(ITSASessionKeys.NINO) must contain(TestConstants.testNino)
        session.get(ITSASessionKeys.UTR) must contain(TestConstants.testUtr)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(UserMatched.name)
        result.verifyStoredUserDetailsIs(None)(r)

        verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsIndividual, None, userDetails, 0, lockedOut = false))
      }
    }

    "UserMatchingService returns user with only nino" should {
      s"redirect to the home controller with nino added to session" in {
        mockUserMatchSuccessNoUtr(userDetails)
        setupMockNotLockedOut(testCredId)

        val r = request.buildRequest(userDetails)

        val result = await(callSubmit(r))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.index.url)

        val session = result.session(request)
        session.get(ITSASessionKeys.NINO) must contain(TestConstants.testNino)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(UserMatched.name)

        verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsIndividual, None, userDetails, 0, lockedOut = false))
      }
    }

    "UserMatchingService returns nothing" when {
      "not locked out is returned by the service" should {
        "redirect to the user details error page and apply the new counter to session" in {
          mockUserMatchNotFound(userDetails)
          setupMockNotLockedOut(testCredId)
          setupIncrementNotLockedOut(testCredId, 0)

          val r = request.buildRequest(userDetails)

          val result = await(callSubmit(r))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.usermatching.routes.UserDetailsErrorController.show().url)

          val session = result.session(request)
          session.get(ITSASessionKeys.FailedUserMatching) must contain("1")

          verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsIndividual, None, userDetails, 1, lockedOut = false))
        }
      }

      "locked out is returned by the service" should {
        "remove the counter from the session, lockout the user then redirect to the locked out page" in {
          val currentFailedMatches = appConfig.matchingAttempts - 1
          implicit val requestWithLockout: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
            ITSASessionKeys.FailedUserMatching -> currentFailedMatches.toString
          )

          mockUserMatchNotFound(userDetails)
          setupMockNotLockedOut(testCredId)
          setupIncrementLockedOut(testCredId, currentFailedMatches)
          mockDeleteAllFromSubscriptionDetails(HttpResponse(Status.OK))

          val r = requestWithLockout.buildRequest(userDetails)

          val result = await(TestConfirmUserController.submit()(r))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.usermatching.routes.UserDetailsLockoutController.show.url)

          val session = result.session
          session.get(ITSASessionKeys.FailedUserMatching) mustBe None

          verifyIncrementLockout(testCredId, 1)
          verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsIndividual, None, userDetails, 0, lockedOut = true))
        }
      }
    }
  }


  authorisationTests()

}