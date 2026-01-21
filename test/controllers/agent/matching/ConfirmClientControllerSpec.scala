/*
 * Copyright 2023 HM Revenue & Customs
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

import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.FailedClientMatching
import connectors.httpparser.SaveSessionDataHttpParser
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockClientDetailsJourneyRefiner, MockIdentifierAction}
import controllers.agent.resolvers.MockAlreadySignedUpResolver
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.http.Status
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import services.agent.*
import services.mocks.*
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingTestSupport
import utilities.agent.TestConstants.{testNino, testUtr}
import utilities.agent.{TestConstants, TestModels}
import views.html.agent.matching.CheckYourClientDetails

import scala.concurrent.Future

class ConfirmClientControllerSpec extends ControllerSpec
  with MockUserLockoutService
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockSessionDataService
  with MockIdentifierAction
  with MockClientDetailsJourneyRefiner
  with MockAlreadySignedUpResolver
  with UserMatchingTestSupport {

  lazy val mockAgentQualificationService: AgentQualificationService = mock[AgentQualificationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentQualificationService)
    mockResolverNoChannel()
  }

  def mockOrchestrateAgentQualificationSuccess(arn: String, nino: String, utr: Option[String], preExistingRelationship: Boolean = true): Unit =
    when(mockAgentQualificationService.orchestrateAgentQualification(
      ArgumentMatchers.eq(TestModels.testClientDetails),
      ArgumentMatchers.eq(arn)
    )(
      ArgumentMatchers.any(),
      ArgumentMatchers.any())
    ).thenReturn(Future.successful(if (preExistingRelationship) Right(ApprovedAgent(nino, utr)) else Left(UnApprovedAgent(nino, utr))))

  def mockOrchestrateAgentQualificationFailure(arn: String, expectedResult: UnqualifiedAgent): Unit =
    when(mockAgentQualificationService.orchestrateAgentQualification(
      ArgumentMatchers.eq(TestModels.testClientDetails),
      ArgumentMatchers.eq(arn)
    )(
      ArgumentMatchers.any(),
      ArgumentMatchers.any())
    ).thenReturn(Future.successful(Left(expectedResult)))

  lazy val utr: String = TestConstants.testUtr
  lazy val nino: String = TestConstants.testNino

  val builtRequest: FakeRequest[AnyContent] = request.buildRequest(Some(TestModels.testClientDetails))

  "show" should {

    def call(controller: ConfirmClientController, request: Request[AnyContent]): Future[Result] = controller.show()(request)

    "redirect to client details" when {
      "client details are missing from the session" in withController { controller =>
        setupMockNotLockedOut(testARN)

        val result = call(controller, request)

        status(result) must be(Status.SEE_OTHER)

        await(result).verifyStoredUserDetailsIs(None)(request)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }

    "return ok (200)" when {
      "client details are in the session" in withController { controller =>
        setupMockNotLockedOut(testARN)

        val r = request.buildRequest(Some(TestModels.testClientDetails))

        val result = call(controller, r)

        status(result) must be(Status.OK)

        await(result).verifyStoredUserDetailsIs(Some(TestModels.testClientDetails))(r)
      }
    }

    "throw an exception" when {
      "the lockout check fails" in withController { controller =>
        setupMockLockStatusFailureResponse(testARN)

        val r = request.buildRequest(Some(TestModels.testClientDetails))

        val result = call(controller, r)

        intercept[InternalServerException](await(result)).getMessage mustBe "[ClientDetailsLockoutController][handleLockOut] lockout status failure"
      }
    }
  }

  "submit" when {
    "the agent is locked out" should {
      "redirect to the lockout route" in withController { controller =>
        setupMockLockedOut(testARN)

        val result: Future[Result] = controller.submit()(builtRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ClientDetailsLockoutController.show.url)
      }
    }
    "the lockout status returned an unexpected error" should {
      "throw an internal server exception" in withController { controller =>
        setupMockLockStatusFailureResponse(testARN)

        intercept[InternalServerException](await(controller.submit()(builtRequest)))
          .message mustBe "[ClientDetailsLockoutController][handleLockOut] lockout status failure"
      }
    }
    "the agent is not locked out" when {
      "the agent has no client details saved" should {
        "redirect to the enter client details page" in withController { controller =>
          setupMockNotLockedOut(testARN)

          val result: Future[Result] = controller.submit()(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ClientDetailsController.show().url)
        }
      }
      "the agent has a set of client details saved" when {
        "there was no client which matched the details entered" should {
          "redirect to the client details error page" when {
            "the agent has not been locked out" in withController { controller =>
              setupMockNotLockedOut(testARN)
              mockOrchestrateAgentQualificationFailure(testARN, NoClientMatched)
              setupIncrementNotLockedOut(testARN, 0, 3)

              val result: Future[Result] = controller.submit()(builtRequest)

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(routes.ClientDetailsErrorController.show.url)
            }
          }
          "redirect to the client details lockout page" when {
            "the agent has been locked out" in withController { controller =>
              setupMockNotLockedOut(testARN)
              mockOrchestrateAgentQualificationFailure(testARN, NoClientMatched)
              setupIncrementLockedOut(testARN, 2, 3)

              val lockoutRequest = builtRequest.withSession(builtRequest.session.data ++: Seq(FailedClientMatching -> 2.toString): _*)

              val result: Future[Result] = controller.submit()(lockoutRequest)

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(routes.ClientDetailsLockoutController.show.url)
            }
          }
          "throw an internal server exception" when {
            "there was a problem incrementing the lockout counter" in withController { controller =>
              setupMockNotLockedOut(testARN)
              mockOrchestrateAgentQualificationFailure(testARN, NoClientMatched)
              setupIncrementLockedOutFailure(testARN, 0)

              intercept[InternalServerException](await(controller.submit()(builtRequest)))
                .message mustBe "ConfirmClientController.lockUser failure"
            }
          }
        }
        "the client entered has already been signed up" should {
          "redirect to resolver" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationFailure(testARN, ClientAlreadySubscribed(None))

            val result: Future[Result] = controller.submit()(builtRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(resolverUrl)
          }
        }
        "there was an unexpected failure checking the agent qualification" should {
          "throw an internal server exception" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationFailure(testARN, UnexpectedFailure)

            intercept[InternalServerException](await(controller.submit()(builtRequest)))
              .message mustBe "[ConfirmClientController][handleUnexpectedFailure] - orchestrate agent qualification failed with an unexpected failure"
          }
        }
        "the agent is not authorised to act on behalf of this client" should {
          "redirect to the unauthorised agent page" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationFailure(testARN, UnApprovedAgent(testNino, Some(testUtr)))

            when(mockSessionDataService.saveNino(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(SaveSessionDataSuccessResponse)))

            val result: Future[Result] = controller.submit()(builtRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.NoClientRelationshipController.show.url)

            session(result).get(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED) mustBe Some("true")
          }
        }
        "the client is not signed up for self assessment as they have no utr" should {
          "redirect to the no sa page" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationSuccess(testARN, testNino, None)

            val result: Future[Result] = controller.submit()(builtRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.NoSAController.show.url)
          }
        }
        "the agent successfully matches against their client" when {
          "saving the nino to session was successful" when {
            "saving the utr to session was successful" should {
              "redirect to the confirmed client resolver" in withController { controller =>
                setupMockNotLockedOut(testARN)
                mockOrchestrateAgentQualificationSuccess(testARN, testNino, Some(testUtr))
                mockSaveNino(testNino)(Right(SaveSessionDataSuccessResponse))
                mockSaveUTR(testUtr)(Right(SaveSessionDataSuccessResponse))

                val result: Future[Result] = controller.submit()(builtRequest)

                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(routes.ConfirmedClientResolver.resolve.url)
              }
            }
            "saving the utr to session had a failure" should {
              "throw an InternalServerException" in withController { controller =>
                setupMockNotLockedOut(testARN)
                mockOrchestrateAgentQualificationSuccess(testARN, testNino, Some(testUtr))
                mockSaveNino(testNino)(Right(SaveSessionDataSuccessResponse))
                mockSaveUTR(testUtr)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

                intercept[InternalServerException](await(controller.submit()(builtRequest)))
                  .message mustBe "[ConfirmClientController][handleApprovedAgent] - failure when saving utr to session"
              }
            }
          }
          "saving the nino to session had a failure" should {
            "throw an InternalServerException" in withController { controller =>
              setupMockNotLockedOut(testARN)
              mockOrchestrateAgentQualificationSuccess(testARN, testNino, Some(testUtr))
              mockSaveNino(testNino)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

              intercept[InternalServerException](await(controller.submit()(builtRequest)))
                .message mustBe "[ConfirmClientController][handleApprovedAgent] - failure when saving nino to session"
            }
          }
        }
      }
    }
  }

  private def withController(testCode: ConfirmClientController => Any): Unit = {
    val checkYourClientDetailsView = mock[CheckYourClientDetails]
    when(checkYourClientDetailsView(any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)
    val controller = createTestConfirmClientController(checkYourClientDetailsView)
    testCode(controller)
  }

  private def createTestConfirmClientController(mockedView: CheckYourClientDetails) = new ConfirmClientController(
    fakeIdentifierAction,
    fakeClientDetailsJourneyRefiner,
    mockAuditingService,
    mockedView,
    mockAgentQualificationService,
    mockSessionDataService,
    mockResolver,
    mockUserLockoutService
  )

}
