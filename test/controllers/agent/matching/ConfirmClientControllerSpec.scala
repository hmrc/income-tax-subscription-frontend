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

import common.Constants.ITSASessionKeys.FailedClientMatching
import connectors.httpparser.SaveSessionDataHttpParser
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import controllers.agent.AgentControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers.{await, _}
import play.twirl.api.HtmlFormat
import services.agent._
import services.mocks._
import uk.gov.hmrc.http.InternalServerException
import utilities.agent.TestConstants.{testARN, testNino, testUtr}
import utilities.agent.{TestConstants, TestModels}
import views.html.agent.matching.CheckYourClientDetails

import scala.concurrent.Future

class ConfirmClientControllerSpec extends AgentControllerBaseSpec
  with MockUserLockoutService
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockSessionDataService {

  override val controllerName: String = "ConfirmClientController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmClientController.show(),
    "submit" -> TestConfirmClientController.submit()
  )

  lazy val mockAgentQualificationService: AgentQualificationService = mock[AgentQualificationService]

  private lazy val TestConfirmClientController = createTestConfirmClientController(mock[CheckYourClientDetails])

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
    ).thenReturn(Future.successful(if (preExistingRelationship) Right(ApprovedAgent(nino, utr)) else Left(UnApprovedAgent(nino, utr))))

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

  private lazy val request = userMatchingRequest.buildRequest(Some(TestModels.testClientDetails))

  "show" should {

    def call(controller: ConfirmClientController, request: Request[AnyContent]): Future[Result] = controller.show()(request)

    "redirect to client details" when {
      "client details are missing from the session" in withController { controller =>
        setupMockNotLockedOut(arn)

        val result = call(controller, userMatchingRequest)

        status(result) must be(Status.SEE_OTHER)

        await(result).verifyStoredUserDetailsIs(None)(userMatchingRequest)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }

    "return ok (200)" when {
      "client details are in the session" in withController { controller =>
        setupMockNotLockedOut(arn)

        val r = request.buildRequest(Some(TestModels.testClientDetails))

        val result = call(controller, r)

        status(result) must be(Status.OK)

        await(result).verifyStoredUserDetailsIs(Some(TestModels.testClientDetails))(r)
      }
    }

    "throw an exception" when {
      "the lockout check fails" in withController { controller =>
        setupMockLockStatusFailureResponse(arn)

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

        val result: Future[Result] = controller.submit()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ClientDetailsLockoutController.show.url)
      }
    }
    "the lockout status returned an unexpected error" should {
      "throw an internal server exception" in withController { controller =>
        setupMockLockStatusFailureResponse(testARN)

        intercept[InternalServerException](await(controller.submit()(request)))
          .message mustBe "[ClientDetailsLockoutController][handleLockOut] lockout status failure"
      }
    }
    "the agent is not locked out" when {
      "the agent has no client details saved" should {
        "redirect to the enter client details page" in withController { controller =>
          setupMockNotLockedOut(testARN)

          val result: Future[Result] = controller.submit()(userMatchingRequest)

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
              setupIncrementNotLockedOut(testARN, 0)

              val result: Future[Result] = controller.submit()(request)

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(routes.ClientDetailsErrorController.show.url)
            }
          }
          "redirect to the client details lockout page" when {
            "the agent has been locked out" in withController { controller =>
              setupMockNotLockedOut(testARN)
              mockOrchestrateAgentQualificationFailure(testARN, NoClientMatched)
              setupIncrementLockedOut(testARN, 2)

              val result: Future[Result] = controller.submit()(request.addingToSession(FailedClientMatching -> 2.toString))

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(routes.ClientDetailsLockoutController.show.url)
            }
          }
          "throw an internal server exception" when {
            "there was a problem incrementing the lockout counter" in withController { controller =>
              setupMockNotLockedOut(testARN)
              mockOrchestrateAgentQualificationFailure(testARN, NoClientMatched)
              setupIncrementLockedOutFailure(testARN, 0)

              intercept[InternalServerException](await(controller.submit()(request)))
                .message mustBe "ConfirmClientController.lockUser failure"
            }
          }
        }
        "the client entered has already been signed up" should {
          "redirect to the client already signed up page" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationFailure(testARN, ClientAlreadySubscribed)

            val result: Future[Result] = controller.submit()(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.ClientAlreadySubscribedController.show.url)
          }
        }
        "there was an unexpected failure checking the agent qualification" should {
          "throw an internal server exception" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationFailure(testARN, UnexpectedFailure)

            intercept[InternalServerException](await(controller.submit()(request)))
              .message mustBe "[ConfirmClientController][handleUnexpectedFailure] - orchestrate agent qualification failed with an unexpected failure"
          }
        }
        "the agent is not authorised to act on behalf of this client" should {
          "redirect to the unauthorised agent page" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationFailure(testARN, UnApprovedAgent(testNino, Some(testUtr)))

            val result: Future[Result] = controller.submit()(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.NoClientRelationshipController.show.url)
          }
        }
        "the client is not signed up for self assessment as they have no utr" should {
          "redirect to the no sa page" in withController { controller =>
            setupMockNotLockedOut(testARN)
            mockOrchestrateAgentQualificationSuccess(testARN, testNino, None)

            val result: Future[Result] = controller.submit()(request)

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

                val result: Future[Result] = controller.submit()(request)

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

                intercept[InternalServerException](await(controller.submit()(request)))
                  .message mustBe "[ConfirmClientController][handleApprovedAgent] - failure when saving utr to session"
              }
            }
          }
          "saving the nino to session had a failure" should {
            "throw an InternalServerException" in withController { controller =>
              setupMockNotLockedOut(testARN)
              mockOrchestrateAgentQualificationSuccess(testARN, testNino, Some(testUtr))
              mockSaveNino(testNino)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

              intercept[InternalServerException](await(controller.submit()(request)))
                .message mustBe "[ConfirmClientController][handleApprovedAgent] - failure when saving nino to session"
            }
          }
        }
      }
    }
  }

  authorisationTests()

  private def withController(testCode: ConfirmClientController => Any): Unit = {
    val checkYourClientDetailsView = mock[CheckYourClientDetails]
    when(checkYourClientDetailsView(any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)
    val controller = createTestConfirmClientController(checkYourClientDetailsView)
    testCode(controller)
  }

  private def createTestConfirmClientController(mockedView: CheckYourClientDetails) = new ConfirmClientController(
    mockedView,
    mockAgentQualificationService,
    mockUserLockoutService
  )(
    mockAuditingService,
    mockAuthService,
    mockSessionDataService,
    appConfig,
    MockSubscriptionDetailsService
  )
}
