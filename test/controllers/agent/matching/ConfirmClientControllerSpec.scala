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

package controllers.agent.matching

import auth.agent.AgentUserMatched
import connectors.individual.eligibility.httpparsers.{Eligible, Ineligible}
import controllers.agent.{AgentControllerBaseSpec, ITSASessionKeys}
import models.audits.EnterDetailsAuditing
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import play.twirl.api.HtmlFormat
import services.agent._
import services.agent.mocks.MockAgentQualificationService
import services.mocks.{MockGetEligibilityStatusService, MockSubscriptionDetailsService, MockUserLockoutService}
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}
import utilities.UserMatchingSessionUtil
import utilities.agent.TestModels.testClientDetails
import utilities.agent.{TestConstants, TestModels}
import views.html.agent.CheckYourClientDetails

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

  lazy val TestConfirmClientController = createTestConfirmClientController(mock[CheckYourClientDetails])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentQualificationService)
  }

  def mockOrchestrateAgentQualificationSuccess(arn: String, nino: String, utr: Option[String], preExistingRelationship: Boolean = true): Unit =
    when(mockAgentQualificationService.orchestrateAgentQualification(
      ArgumentMatchers.eq(TestModels.testClientDetails),
      ArgumentMatchers.eq(arn)
    )(
      ArgumentMatchers.any(),
      ArgumentMatchers.any())
    ).thenReturn(Future.successful(if (preExistingRelationship) Right(ApprovedAgent(nino, utr)) else Right(UnApprovedAgent(nino, utr))))

  def mockOrchestrateAgentQualificationFailure(arn: String, expectedResult: UnqualifiedAgent): Unit =
    when(mockAgentQualificationService.orchestrateAgentQualification(
      ArgumentMatchers.eq(TestModels.testClientDetails),
      ArgumentMatchers.eq(arn)
    )(
      ArgumentMatchers.any(),
      ArgumentMatchers.any())
    ).thenReturn(Future.successful(Left(expectedResult)))

  lazy val arn: String = TestConstants.testARN
  lazy val utr: String = TestConstants.testUtr
  lazy val nino: String = TestConstants.testNino

  lazy val request = userMatchingRequest.buildRequest(TestModels.testClientDetails)

  "Calling the show action of the ConfirmClientController with an authorised user" should {

    def call(controller: ConfirmClientController, request: Request[AnyContent]): Future[Result] = controller.show()(request)

    "when there are no client details store redirect them to client details" in withController { controller =>
      setupMockNotLockedOut(arn)

      val result = call(controller, userMatchedRequest)

      status(result) must be(Status.SEE_OTHER)

      await(result).verifyStoredUserDetailsIs(None)(userMatchedRequest)

    }

    "if there is are client details return ok (200)" in withController { controller =>
      setupMockNotLockedOut(arn)

      val r = request.buildRequest(TestModels.testClientDetails)

      val result = call(controller, r)

      status(result) must be(Status.OK)

      await(result).verifyStoredUserDetailsIs(TestModels.testClientDetails)(r)
    }

    "if there is a failure response from the lockout service" in withController { controller =>
      setupMockLockStatusFailureResponse(arn)

      val r = request.buildRequest(TestModels.testClientDetails)

      val result = call(controller, r)

      intercept[InternalServerException](await(result)).getMessage mustBe "[ClientDetailsLockoutController][handleLockOut] lockout status failure"
    }
  }

  "Calling the submit action of the ConfirmClientController with an authorised user and valid submission" when {

    def callSubmit(controller: ConfirmClientController, fakeRequest: FakeRequest[AnyContent] = request): Future[Result] = controller.submit()(fakeRequest)

    "the client details are not in session" should {
      s"redirect user to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in withController { controller =>
        setupMockNotLockedOut(arn)

        val result = callSubmit(controller, userMatchingRequest.buildRequest(None))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }
    "the client details are in session" when {
      "AgentQualificationService returned UnexpectedFailure" should {
        "return a InternalServerException" in withController { controller =>
          mockOrchestrateAgentQualificationFailure(arn, UnexpectedFailure)
          setupMockNotLockedOut(arn)

          val result = callSubmit(controller)

          intercept[InternalServerException](await(result))
          verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 0, lockedOut = false))
        }
      }

      "AgentQualificationService returned NoClientMatched and the agent is not locked out" should {
        s"redirect user to ${controllers.agent.matching.routes.ClientDetailsErrorController.show.url}" in withController { controller =>
          mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
          setupMockNotLockedOut(arn)
          setupIncrementNotLockedOut(arn, 0)

          val result = await(callSubmit(controller))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsErrorController.show.url)
          verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 1, lockedOut = false))
        }
      }

      "AgentQualificationService returned ClientAlreadySubscribed" should {
        s"redirect user to ${controllers.agent.routes.ClientAlreadySubscribedController.show.url}" in withController { controller =>
          mockOrchestrateAgentQualificationFailure(arn, ClientAlreadySubscribed)
          setupMockNotLockedOut(arn)

          val result = await(callSubmit(controller))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.ClientAlreadySubscribedController.show.url)
          verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 0, lockedOut = false))
        }
      }

      "AgentQualificationService returned UnQualifiedAgent" should {
        s"redirect user to ${controllers.agent.routes.NoClientRelationshipController.show.url}" in withController { controller =>
          mockOrchestrateAgentQualificationSuccess(arn, nino, utr, preExistingRelationship = false)
          setupMockNotLockedOut(arn)

          val result = await(callSubmit(controller))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.NoClientRelationshipController.show.url)
          verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 0, lockedOut = false))
        }
      }

      "AgentQualificationService returned ApprovedAgent" when {
        "the user has a utr" when {
          "the client is eligible" should {
            s"redirect user to ${controllers.agent.routes.HomeController.index.url}" in withController { controller =>
              mockOrchestrateAgentQualificationSuccess(arn, nino, utr)
              mockGetEligibilityStatus(utr)(Future.successful(Eligible))
              setupMockNotLockedOut(arn)

              val result = await(callSubmit(controller))

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.agent.routes.HomeController.index.url)

              val session = result.session(request)

              session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatched.name)
              session.get(ITSASessionKeys.NINO) mustBe Some(nino)
              session.get(ITSASessionKeys.UTR) mustBe Some(utr)

              verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 0, lockedOut = false))
            }
          }
          "the client is ineligible" should {
            s"redirect user to ${controllers.agent.eligibility.routes.CannotTakePartController.show.url}" in withController { controller =>
              mockOrchestrateAgentQualificationSuccess(arn, nino, utr)
              mockGetEligibilityStatus(utr)(Future.successful(Ineligible))
              setupMockNotLockedOut(arn)

              val result = await(callSubmit(controller))

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.agent.eligibility.routes.CannotTakePartController.show.url)

              result.verifyStoredUserDetailsIs(None)(request)

              verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 0, lockedOut = false))
            }
          }
        }

        "the user does not have a utr" should {
          s"redirect user to ${controllers.agent.matching.routes.NoSAController.show.url}" in withController { controller =>
            mockOrchestrateAgentQualificationSuccess(arn, nino, None)
            setupMockNotLockedOut(arn)

            val result = await(callSubmit(controller))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.routes.HomeController.index.url)

            val session = result.session(userMatchingRequest.withSession(ITSASessionKeys.UTR -> "this will be deleted"))
            session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatched.name)
            session.get(ITSASessionKeys.NINO) mustBe Some(nino)
            session.get(ITSASessionKeys.UTR) mustBe None

            verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 0, lockedOut = false))
          }
        }
      }
    }
  }

  "An agent who is locked out" should {
    s"be redirect to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show.url} when calling show" in withController { controller =>
      setupMockLockedOut(arn)

      val result = controller.show()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show.url
    }

    s"be redirect to ${controllers.agent.matching.routes.ClientDetailsLockoutController.show.url} when calling submit" in withController { controller =>
      setupMockLockedOut(arn)

      val result = controller.submit()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsLockoutController.show.url
    }
  }

  "An agent who is not yet locked out" when {

    "they fail client matching for the first time" should {

      s"have the ${ITSASessionKeys.FailedClientMatching} -> 1 added to session and go to the client match error page" in withController { controller =>
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)
        setupMockNotLockedOut(arn)
        setupIncrementNotLockedOut(arn, 0)

        val result = await(controller.submit()(request))

        result.session(request).get(ITSASessionKeys.FailedClientMatching) mustBe Some(1.toString)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsErrorController.show.url)

        verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 1, lockedOut = false))
      }

    }

    "they matched a client after failing previously" should {
      s"have the ${ITSASessionKeys.FailedClientMatching} removed from session" in withController { controller =>
        mockOrchestrateAgentQualificationSuccess(arn, nino, utr)
        mockGetEligibilityStatus(utr)(Future.successful(Eligible))
        setupMockNotLockedOut(arn)

        val result = await(controller.submit()(request.withSession(ITSASessionKeys.FailedClientMatching -> 1.toString)))

        result.session(request).get(ITSASessionKeys.FailedClientMatching) mustBe None

        status(result) mustBe SEE_OTHER
        redirectLocation(result) must not be Some(controllers.agent.matching.routes.ClientDetailsLockoutController.show.url)

        verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 1, lockedOut = false))
      }

    }

    s"they failed matching one less than configured max attempts already" should {

      s"have the ${ITSASessionKeys.FailedClientMatching} and client details removed from session" in withController { controller =>
        lazy val prevFailedAttempts = appConfig.matchingAttempts - 1

        setupMockNotLockedOut(arn)
        setupIncrementLockedOut(arn, prevFailedAttempts)
        mockDeleteAllFromSubscriptionDetails(HttpResponse(OK))
        mockOrchestrateAgentQualificationFailure(arn, NoClientMatched)

        val result = await(controller.submit()(request.withSession(ITSASessionKeys.FailedClientMatching -> prevFailedAttempts.toString)))

        val session = result.session(request)
        List(
          ITSASessionKeys.FailedClientMatching,
          UserMatchingSessionUtil.firstName,
          UserMatchingSessionUtil.lastName,
          UserMatchingSessionUtil.dobD,
          UserMatchingSessionUtil.dobM,
          UserMatchingSessionUtil.dobY,
          UserMatchingSessionUtil.nino
        ).foreach(session.get(_) mustBe None)

        verifyIncrementLockout(arn, 1)
        verifyAudit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), testClientDetails, 0, lockedOut = true))
      }
    }
  }

  "The back url" should {
    s"point to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in withController { controller =>
      controller.backUrl mustBe controllers.agent.matching.routes.ClientDetailsController.show().url
    }
  }

  authorisationTests()

  private def withController(testCode: ConfirmClientController => Any): Unit = {
    val checkYourClientDetailsView = mock[CheckYourClientDetails]
    when(checkYourClientDetailsView(any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)
    val controller = createTestConfirmClientController(checkYourClientDetailsView)
    testCode(controller)
  }

  private def createTestConfirmClientController(mockedView: CheckYourClientDetails, enableMatchingFeature: Boolean = false) = new ConfirmClientController(
    mockedView,
    mockAuditingService,
    mockAuthService,
    mockAgentQualificationService,
    mockGetEligibilityStatusService,
    mockUserLockoutService
  )
}
