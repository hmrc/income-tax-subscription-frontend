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

package services.individual.claimenrolment

import auth.individual.IncomeTaxSAUser
import common.Constants
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.agent.CheckEnrolmentAllocationServiceModel.{EnrolmentAlreadyAllocated, EnrolmentNotAllocated, EnrolmentStoreProxyInvalidJsonResponse, UnexpectedEnrolmentStoreProxyFailure}
import services.individual.claimenrolment.ClaimEnrolmentService.*
import services.individual.mocks.{MockEnrolmentService, MockKnownFactsService}
import services.mocks.{MockCheckEnrolmentAllocationService, MockNinoService, MockSessionDataService, MockSubscriptionService}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.*
import utilities.individual.TestConstants.{testEnrolmentKey, testGroupId, testMTDID, testNino}

import scala.concurrent.Future

class ClaimEnrolmentServiceSpec extends PlaySpec
  with Matchers
  with MockSubscriptionService
  with MockCheckEnrolmentAllocationService
  with MockKnownFactsService
  with MockNinoService
  with MockEnrolmentService
  with MockSessionDataService {

  object TestClaimEnrolmentService extends ClaimEnrolmentService(
    mockSubscriptionService,
    mockNinoService,
    mockCheckEnrolmentAllocationService,
    mockKnownFactsService,
    mockEnrolmentService,
    mockSessionDataService
  ) {
    mockGetAllSessionData()
  }

  implicit val request: Request[AnyContent] = FakeRequest()
  private val fullEnrolments: Enrolments = Enrolments(Set(
    Enrolment(
      Constants.ninoEnrolmentName,
      Seq(EnrolmentIdentifier(Constants.ninoEnrolmentIdentifierKey, testNino)),
      "Activated"
    )
  ))
  implicit val user: IncomeTaxSAUser = new IncomeTaxSAUser(
    enrolments = fullEnrolments,
    affinityGroup = Some(Individual),
    credentialRole = Some(User),
    confidenceLevel = ConfidenceLevel.L200,
    userId = "testUserId"
  )
  val userWithNoNino: IncomeTaxSAUser = new IncomeTaxSAUser(
    enrolments = Enrolments(Set.empty),
    affinityGroup = Some(Individual),
    credentialRole = Some(User),
    confidenceLevel = ConfidenceLevel.L200,
    userId = "testUserId"
  )

  "claimEnrolment" when {
    "the user has a nino in their user profile" when {
      "the user is not signed up to mtd income tax" should {
        "return a NotSubscribed" in {
          mockGetNino(testNino)
          setupMockGetSubscriptionNotFound(testNino)

          val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

          await(result) mustBe Left(NotSubscribed)
        }
      }
      "there was an unexpected response when checking if the user is subscribed" should {
        "return a ClaimEnrolmentError" in {
          mockGetNino(testNino)
          setupMockGetSubscriptionFailure(testNino)

          val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

          await(result) mustBe Left(ClaimEnrolmentError(
            msg = s"[ClaimEnrolmentService][getMtditid] - Unexpected response calling get business details. Status: $BAD_REQUEST"
          ))
        }
      }
      "the user is signed up to mtd income tax" when {
        "the enrolment is already allocated" should {
          "return a AlreadySignedUp" in {
            mockGetNino(testNino)
            setupMockGetSubscriptionFound(testNino)
            mockGetGroupIdForEnrolment(testEnrolmentKey)(Left(EnrolmentAlreadyAllocated(testGroupId)))

            val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

            await(result) mustBe Left(AlreadySignedUp)
          }
        }
        "the response was invalid when checking if the enrolment is already allocated" should {
          "return a ClaimEnrolmentError" in {
            mockGetNino(testNino)
            setupMockGetSubscriptionFound(testNino)
            mockGetGroupIdForEnrolment(testEnrolmentKey)(Left(EnrolmentStoreProxyInvalidJsonResponse))

            val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

            await(result) mustBe Left(ClaimEnrolmentError("[ClaimEnrolmentService][getEnrolmentAllocation] - Unable to parse response"))
          }
        }
        "there was an unexpected response when checking if the enrolment is already allocated" should {
          "return a ClaimEnrolmentError" in {
            mockGetNino(testNino)
            setupMockGetSubscriptionFound(testNino)
            mockGetGroupIdForEnrolment(testEnrolmentKey)(Left(UnexpectedEnrolmentStoreProxyFailure(INTERNAL_SERVER_ERROR)))

            val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

            await(result) mustBe Left(ClaimEnrolmentError(
              msg = s"[ClaimEnrolmentService][getEnrolmentAllocation] - Unexpected response. Status: $INTERNAL_SERVER_ERROR"
            ))
          }
        }
        "the enrolment is not allocated elsewhere" when {
          "there was a problem adding known facts for the enrolment" should {
            "return a ClaimEnrolmentError" in {
              mockGetNino(testNino)
              setupMockGetSubscriptionFound(testNino)
              mockGetGroupIdForEnrolment(testEnrolmentKey)(Right(EnrolmentNotAllocated))
              mockAddKnownFactsFailure(mtditid = testMTDID, nino = testNino)

              val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

              await(result) mustBe Left(ClaimEnrolmentError(
                msg = "[ClaimEnrolmentService][addKnownFacts] - Unexpected response whilst adding known facts. Response: This is an error"
              ))
            }
          }
          "adding known facts for the enrolment was successful" when {
            "there was a problem allocating the enrolment" should {
              "return a ClaimEnrolmentError" in {
                mockGetNino(testNino)
                setupMockGetSubscriptionFound(testNino)
                mockGetGroupIdForEnrolment(testEnrolmentKey)(Right(EnrolmentNotAllocated))
                mockAddKnownFactsSuccess(mtditid = testMTDID, nino = testNino)
                mockEnrolFailure(mtditid = testMTDID, nino = testNino)

                val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

                await(result) mustBe Left(ClaimEnrolmentError(
                  msg = "[ClaimEnrolmentService][allocateEnrolment] - Unexpected response whist allocating enrolment. Response: This is an error"
                ))
              }
            }
            "allocating the enrolment was successful" should {
              "return a ClaimEnrolmentSuccess" in {
                mockGetNino(testNino)
                setupMockGetSubscriptionFound(testNino)
                mockGetGroupIdForEnrolment(testEnrolmentKey)(Right(EnrolmentNotAllocated))
                mockAddKnownFactsSuccess(mtditid = testMTDID, nino = testNino)
                mockEnrolSuccess(mtditid = testMTDID, nino = testNino)

                val result: Future[ClaimEnrolmentResponse] = TestClaimEnrolmentService.claimEnrolment

                await(result) mustBe Right(ClaimEnrolmentSuccess(testNino, testMTDID))
              }
            }
          }
        }
      }
    }
  }

}
