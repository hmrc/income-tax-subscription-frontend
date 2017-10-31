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

package agent.controllers.matching

import agent.auth.AgentUserMatched
import agent.controllers.ITSASessionKeys
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{await, _}
import agent.services._
import agent.services.mocks.{MockAgentLockoutService, MockAgentQualificationService}
import agent.utils.{TestConstants, TestModels}
import core.controllers.ControllerBaseSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}

class ConfirmClientControllerSpec extends ControllerBaseSpec
  with MockAgentQualificationService
  with MockAgentLockoutService {

  override val controllerName: String = "ConfirmClientController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmClientController.show(),
    "submit" -> TestConfirmClientController.submit()
  )

  lazy val mockAgentQualificationService: AgentQualificationService = mock[AgentQualificationService]

  object TestConfirmClientController extends ConfirmClientController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAgentQualificationService,
    mockAuthService,
    mockAgentLockoutService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentQualificationService)
  }

  def mockOrchestrateAgentQualificationSuccess(arn: String, nino: String): Unit =
    when(mockAgentQualificationService.orchestrateAgentQualification(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(ApprovedAgent(nino))))

  def mockOrchestrateAgentQualificationFailure(arn: String, expectedResult: UnqualifiedAgent): Unit =
    when(mockAgentQualificationService.orchestrateAgentQualification(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(expectedResult)))

  val arn = TestConstants.testARN
  val nino = TestConstants.testNino

  lazy val request = userMatchingRequest

  "Calling the show action of the ConfirmClientController with an authorised user" should {

    def call = TestConfirmClientController.show()(request)

    "when there are no client details store redirect them to client details" in {
      setupMockKeystore(fetchClientDetails = None)
      setupMockNotLockedOut(arn)

      val result = call

      status(result) must be(Status.SEE_OTHER)

      await(result)
      verifyKeystore(fetchClientDetails = 1, saveClientDetails = 0)

    }

    "if there is are client details return ok (200)" in {
      setupMockKeystore(fetchClientDetails = TestModels.testClientDetails)
      setupMockNotLockedOut(arn)

      val result = call

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchClientDetails = 1, saveClientDetails = 0)
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
      s"redirect user to ${agent.controllers.matching.routes.ClientDetailsController.show().url}" in {
        mockOrchestrateAgentQualificationFailure(arn, NoClientDetails)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ClientDetailsController.show().url)
      }
    }

    "AgentQualificationService returned NoClientMatched" should {
      s"redirect user to ${agent.controllers.matching.routes.ClientDetailsErrorController.show().url}" in {
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ClientDetailsErrorController.show().url)
      }
    }

    "AgentQualificationService returned ClientAlreadySubscribed" should {
      s"redirect user to ${agent.controllers.routes.ClientAlreadySubscribedController.show().url}" in {
        mockOrchestrateAgentQualificationFailure(arn, ClientAlreadySubscribed)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(agent.controllers.routes.ClientAlreadySubscribedController.show().url)
      }
    }

    "AgentQualificationService returned NoClientRelationship" should {
      s"redirect user to ${agent.controllers.routes.ClientAlreadySubscribedController.show().url}" in {
        mockOrchestrateAgentQualificationFailure(arn, ClientAlreadySubscribed)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(agent.controllers.routes.ClientAlreadySubscribedController.show().url)
      }
    }

    "AgentQualificationService returned ApprovedAgent" should {
      s"redirect user to ${agent.controllers.routes.HomeController.index().url}" in {
        mockOrchestrateAgentQualificationSuccess(arn, nino)
        setupMockNotLockedOut(arn)

        val result = callSubmit()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(agent.controllers.routes.HomeController.index().url)

        await(result).session(userMatchingRequest).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatched.name)
      }
    }
  }

  "An agent who is locked out" should {
    s"be redirect to ${agent.controllers.matching.routes.ClientDetailsLockoutController.show().url} when calling show" in {
      setupMockLockedOut(arn)

      val result = TestConfirmClientController.show()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe agent.controllers.matching.routes.ClientDetailsLockoutController.show().url
    }

    s"be redirect to ${agent.controllers.matching.routes.ClientDetailsLockoutController.show().url} when calling submit" in {
      setupMockLockedOut(arn)

      val result = TestConfirmClientController.submit()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe agent.controllers.matching.routes.ClientDetailsLockoutController.show().url
    }
  }

  "An agent who is not yet locked out" when {

    "they fail client matching for the first time" should {
      def callSubmit(): Future[Result] = TestConfirmClientController.submit()(request)

      lazy val result = callSubmit()

      s"have the ${ITSASessionKeys.FailedClientMatching} -> 1 added to session" in {
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
        setupMockNotLockedOut(arn)

        await(result).session(request).get(ITSASessionKeys.FailedClientMatching) mustBe Some(1.toString)
      }

      s"redirect to ${agent.controllers.matching.routes.ClientDetailsErrorController.show().url}" in {
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ClientDetailsErrorController.show().url)
      }
    }

    "they matched a client after failing previously" should {
      def callSubmit(): Future[Result] = TestConfirmClientController.submit()(request.withSession(ITSASessionKeys.FailedClientMatching -> 1.toString))

      lazy val result = callSubmit()

      s"have the ${ITSASessionKeys.FailedClientMatching} removed from session" in {
        mockOrchestrateAgentQualificationSuccess(arn, nino)
        setupMockNotLockedOut(arn)

        await(result).session(request).get(ITSASessionKeys.FailedClientMatching) mustBe None
      }

      s"should not be redirected to ${agent.controllers.matching.routes.ClientDetailsLockoutController.show().url}" in {
        status(result) mustBe SEE_OTHER
        redirectLocation(result) must not be Some(agent.controllers.matching.routes.ClientDetailsLockoutController.show().url)
      }
    }

    lazy val prevFailedAttempts = appConfig.matchingAttempts - 1
    s"they failed matching $prevFailedAttempts consecretively already" should {
      def callSubmit(): Future[Result] =
        TestConfirmClientController.submit()(request.withSession(ITSASessionKeys.FailedClientMatching -> prevFailedAttempts.toString))

      def fixture(): Unit = {
        setupMockNotLockedOut(arn)
        setupMockLockCreated(arn)
        setupMockKeystore(deleteAll = HttpResponse(OK))
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
      }

      s"have the ${ITSASessionKeys.FailedClientMatching} removed from session" in {
        fixture()

        val result = callSubmit()

        await(result).session(request).get(ITSASessionKeys.FailedClientMatching) mustBe None
      }

      "removed all data in keystore" in {
        fixture()

        val result = callSubmit()

        await(result)
        verifyKeystore(deleteAll = 1)
      }

      "added lock for the user" in {
        fixture()

        val result = callSubmit()

        await(result)
        verifyLockoutAgent(arn, 1)
      }
    }


  }

  "The back url" should {
    s"point to ${agent.controllers.matching.routes.ClientDetailsController.show().url}" in {
      TestConfirmClientController.backUrl mustBe agent.controllers.matching.routes.ClientDetailsController.show().url
    }
  }

  authorisationTests()
}
