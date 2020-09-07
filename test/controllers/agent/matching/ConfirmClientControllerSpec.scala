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

import auth.agent.AgentUserMatched
import connectors.individual.eligibility.httpparsers.{Eligible, Ineligible}
import controllers.agent.{AgentControllerBaseSpec, ITSASessionKeys}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers.{await, _}
import services.agent._
import services.agent.mocks.MockAgentQualificationService
import services.mocks.{MockGetEligibilityStatusService, MockSubscriptionDetailsService, MockUserLockoutService}
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}
import utilities.agent.{TestConstants, TestModels}

import scala.concurrent.Future

class ConfirmClientControllerSpec extends AgentControllerBaseSpec
  with MockAgentQualificationService
  with MockUserLockoutService
  with MockSubscriptionDetailsService
  with MockGetEligibilityStatusService {

  override val controllerName: String = "ConfirmClientController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmClientController.show(),
    "submit" -> TestConfirmClientController.submit()
  )

  lazy val mockAgentQualificationService: AgentQualificationService = mock[AgentQualificationService]

  private def createTestConfirmClientController(enableMatchingFeature: Boolean = false) = new ConfirmClientController(
    mockAuthService,
    mockAgentQualificationService,
    mockGetEligibilityStatusService,
    mockUserLockoutService
  )

  lazy val TestConfirmClientController = createTestConfirmClientController()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentQualificationService)
  }

  def mockOrchestrateAgentQualificationSuccess(arn: String, nino: String, utr: Option[String], preExistingRelationship: Boolean = true): Unit =

    when(mockAgentQualificationService.orchestrateAgentQualification(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(if (preExistingRelationship) Right(ApprovedAgent(nino, utr)) else Right(UnApprovedAgent(nino, utr))))

  def mockOrchestrateAgentQualificationFailure(arn: String, expectedResult: UnqualifiedAgent): Unit =
    when(mockAgentQualificationService.orchestrateAgentQualification(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(expectedResult)))

  lazy val arn: String = TestConstants.testARN
  lazy val utr: String = TestConstants.testUtr
  lazy val nino: String = TestConstants.testNino

  lazy val request = userMatchingRequest.buildRequest(TestModels.testClientDetails)

  "Calling the show action of the ConfirmClientController with an authorised user" should {

    def call(request: Request[AnyContent]): Future[Result] = TestConfirmClientController.show()(request)

    "when there are no client details store redirect them to client details" in {
      setupMockNotLockedOut(arn)

      val result = call(userMatchedRequest)

      status(result) must be(Status.SEE_OTHER)

      await(result).verifyStoredUserDetailsIs(None)(userMatchedRequest)

    }

    "if there is are client details return ok (200)" in {
      setupMockNotLockedOut(arn)

      val r = request.buildRequest(TestModels.testClientDetails)

      val result = call(r)

      status(result) must be(Status.OK)

      await(result).verifyStoredUserDetailsIs(TestModels.testClientDetails)(r)
    }

    "if there is a failure response from the lockout service" in {
      setupMockLockStatusFailureResponse(arn)

      val r = request.buildRequest(TestModels.testClientDetails)

      val result = call(r)

      intercept[InternalServerException](await(result)).getMessage mustBe "[ClientDetailsLockoutController][handleLockOut] lockout status failure"
    }
  }

  "Calling the submit action of the ConfirmClientController with an authorised user and valid submission" when {

    def callSubmit(): Future[Result] = TestConfirmClientController.submit()(request)

    "AgentQualificationService returned UnexpectedFailure" should {
      "return a InternalServerException" in {
        mockOrchestrateAgentQualificationFailure(arn, UnexpectedFailure)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        intercept[InternalServerException](await(result))
      }
    }

    "AgentQualificationService returned NoClientDetails" should {
      s"redirect user to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in {
        mockOrchestrateAgentQualificationFailure(arn, NoClientDetails)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }

    "AgentQualificationService returned NoClientMatched and the agent is not locked out" should {
      s"redirect user to ${controllers.agent.matching.routes.ClientDetailsErrorController.show().url}" in {
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
        setupMockNotLockedOut(arn)
        setupIncrementNotLockedOut(arn, 0)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsErrorController.show().url)
      }
    }

    "AgentQualificationService returned ClientAlreadySubscribed" should {
      s"redirect user to ${controllers.agent.routes.ClientAlreadySubscribedController.show().url}" in {
        mockOrchestrateAgentQualificationFailure(arn, ClientAlreadySubscribed)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.ClientAlreadySubscribedController.show().url)
      }
    }

    "AgentQualificationService returned UnQualifiedAgent" should {
      s"redirect user to ${controllers.agent.routes.NoClientRelationshipController.show().url}" in {
        mockOrchestrateAgentQualificationSuccess(arn, nino, utr, preExistingRelationship = false)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.NoClientRelationshipController.show().url)
      }
    }

    "AgentQualificationService returned ApprovedAgent" when {
      "the user has a utr" when {
        "the client is eligible" should {
          s"redirect user to ${controllers.agent.routes.HomeController.index().url}" in {
            mockOrchestrateAgentQualificationSuccess(arn, nino, utr)
            mockGetEligibilityStatus(utr)(Future.successful(Eligible))
            setupMockNotLockedOut(arn)

            val fresult = callSubmit()

            status(fresult) mustBe SEE_OTHER
            redirectLocation(fresult) mustBe Some(controllers.agent.routes.HomeController.index().url)

            val result = await(fresult)
            val session = result.session(request)

            session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatched.name)
            session.get(ITSASessionKeys.NINO) mustBe Some(nino)
            session.get(ITSASessionKeys.UTR) mustBe Some(utr)
            result.verifyStoredUserDetailsIs(None)(request)
          }
        }
        "the client is ineligible" should {
          s"redirect user to ${controllers.agent.eligibility.routes.CannotTakePartController.show().url}" in {
            mockOrchestrateAgentQualificationSuccess(arn, nino, utr)
            mockGetEligibilityStatus(utr)(Future.successful(Ineligible))
            setupMockNotLockedOut(arn)

            val fresult = callSubmit()

            status(fresult) mustBe SEE_OTHER
            redirectLocation(fresult) mustBe Some(controllers.agent.eligibility.routes.CannotTakePartController.show().url)

            val result = await(fresult)
            val session = result.session(request)

            result.verifyStoredUserDetailsIs(None)(request)
          }
        }
      }

      "the user does not have a utr" should {
        s"redirect user to ${controllers.agent.matching.routes.NoSAController.show().url}" in {
          mockOrchestrateAgentQualificationSuccess(arn, nino, None)
          setupMockNotLockedOut(arn)

          val result = callSubmit()

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.HomeController.index().url)

          val session = await(result).session(userMatchingRequest.withSession(ITSASessionKeys.UTR -> "this will be deleted"))
          session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatched.name)
          session.get(ITSASessionKeys.NINO) mustBe Some(nino)
          session.get(ITSASessionKeys.UTR) mustBe None
        }
      }
    }
  }

  "An agent who is locked out" should {
    s"be redirect to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show().url} when calling show" in {
      setupMockLockedOut(arn)

      val result = TestConfirmClientController.show()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show().url
    }

    s"be redirect to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show().url} when calling submit" in {
      setupMockLockedOut(arn)

      val result = TestConfirmClientController.submit()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show().url
    }
  }

  "An agent who is not yet locked out" when {

    "they fail client matching for the first time" should {
      def callSubmit(): Future[Result] = TestConfirmClientController.submit()(request)

      lazy val result = callSubmit()

      s"have the ${ITSASessionKeys.FailedClientMatching} -> 1 added to session" in {
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
        setupMockNotLockedOut(arn)
        setupIncrementNotLockedOut(arn, 0)

        await(result).session(request).get(ITSASessionKeys.FailedClientMatching) mustBe Some(1.toString)
      }

      s"redirect to ${controllers.agent.matching.routes.ClientDetailsErrorController.show().url}" in {
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsErrorController.show().url)
      }
    }

    "they matched a client after failing previously" should {
      def callSubmit(): Future[Result] = TestConfirmClientController.submit()(request.withSession(ITSASessionKeys.FailedClientMatching -> 1.toString))

      lazy val result = callSubmit()

      s"have the ${ITSASessionKeys.FailedClientMatching} removed from session" in {
        mockOrchestrateAgentQualificationSuccess(arn, nino, utr)
        mockGetEligibilityStatus(utr)(Future.successful(Eligible))

        setupMockNotLockedOut(arn)

        await(result).session(request).get(ITSASessionKeys.FailedClientMatching) mustBe None
      }

      s"should not be redirected to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show().url}" in {
        mockGetEligibilityStatus(utr)(Future.successful(Eligible))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) must not be Some(controllers.agent.matching.routes.ClientDetailsLockoutController.show().url)
      }
    }

    lazy val prevFailedAttempts = appConfig.matchingAttempts - 1
    s"they failed matching $prevFailedAttempts consecretively already" should {
      def callSubmit(): Future[Result] =
        TestConfirmClientController.submit()(request.withSession(ITSASessionKeys.FailedClientMatching -> prevFailedAttempts.toString))

      def fixture(): Unit = {
        setupMockNotLockedOut(arn)
        setupIncrementLockedOut(arn, prevFailedAttempts)
        mockDeleteAllFromSubscriptionDetails(HttpResponse(OK))
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
      }

      s"have the ${ITSASessionKeys.FailedClientMatching} removed from session" in {
        fixture()

        val result = callSubmit()

        await(result).session(request).get(ITSASessionKeys.FailedClientMatching) mustBe None
      }

      "added lock for the user" in {
        fixture()

        val result = callSubmit()

        await(result)
        verifyIncrementLockout(arn, 1)
      }
    }


  }

  "The back url" should {
    s"point to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in {
      TestConfirmClientController.backUrl mustBe controllers.agent.matching.routes.ClientDetailsController.show().url
    }
  }

  authorisationTests()
}
