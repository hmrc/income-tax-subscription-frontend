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

package services

import connectors.agent.httpparsers.{AllocateEnrolmentResponseHttpParser, GetUsersForGroupHttpParser, QueryUsersHttpParser, UpsertEnrolmentResponseHttpParser}
import connectors.agent.mocks.{MockEnrolmentStoreProxyConnector, MockUsersGroupsSearchConnector}
import models.common.subscription.EnrolmentKey
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers._
import services.agent.AutoEnrolmentService.AutoClaimEnrolmentResponse
import services.agent.{AssignEnrolmentToUserService, AutoEnrolmentService, CheckEnrolmentAllocationService}
import services.mocks.{MockAssignEnrolmentToUserService, MockCheckEnrolmentAllocationService}
import uk.gov.hmrc.auth.core.{Assistant, CredentialRole, User}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.Constants.{utrEnrolmentIdentifierKey, utrEnrolmentName}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AutoEnrolmentServiceSpec extends AnyWordSpec
  with Matchers
  with MockEnrolmentStoreProxyConnector
  with MockUsersGroupsSearchConnector
  with MockCheckEnrolmentAllocationService
  with MockAssignEnrolmentToUserService {

  object TestAutoEnrolmentService extends AutoEnrolmentService(
    enrolmentStoreProxyConnector = mockEnrolmentStoreProxyConnector,
    checkEnrolmentAllocationService = mockCheckEnrolmentAllocationService,
    assignEnrolmentToUserService = mockAssignEnrolmentToUserService,
    usersGroupsSearchConnector = mockUsersGroupsSearchConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()


  val testUtr: String = "1234567890"
  val testSAEnrolment: EnrolmentKey = EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr)
  val testNino: String = "AA123456A"
  val testMtditid: String = "XAIT00000000000"
  val testGroupId: String = UUID.randomUUID().toString
  val testUserId1: String = UUID.randomUUID().toString
  val testUserId2: String = UUID.randomUUID().toString

  "autoClaimEnrolment" must {

    s"return ${AutoEnrolmentService.EnrolmentAssigned}" when {
      "all calls are successful" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Right(QueryUsersHttpParser.UsersFound(Set(testUserId1, testUserId2))))
        mockGetUsersForGroups(testGroupId)(Right(GetUsersForGroupHttpParser.UsersFound(Map[String, CredentialRole](
          testUserId1 -> User,
          testUserId2 -> Assistant
        ))))
        mockEnrolmentStoreUpsertEnrolment(testMtditid, testNino)(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess))
        mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testUserId1, testMtditid)(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess))
        mockAssignEnrolment(Set(testUserId2), testMtditid)(Right(AssignEnrolmentToUserService.EnrolmentAssignedToUsers))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Right(AutoEnrolmentService.EnrolmentAssigned)
      }
    }

    s"return ${AutoEnrolmentService.EnrolmentNotAllocated}" when {
      "the enrolment is not allocated to a group" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.EnrolmentNotAllocated)
      }
    }

    s"return ${AutoEnrolmentService.EnrolmentStoreProxyFailure(INTERNAL_SERVER_ERROR)}" when {
      "the check enrolment allocation service returns an unexpected status error" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.EnrolmentStoreProxyFailure(INTERNAL_SERVER_ERROR))
      }
    }

    s"return ${AutoEnrolmentService.EnrolmentStoreProxyInvalidJsonResponse}" when {
      "the check enrolment allocation service returns an invalid json error" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentStoreProxyInvalidJsonResponse))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.EnrolmentStoreProxyInvalidJsonResponse)
      }
    }

    s"return ${AutoEnrolmentService.NoUsersFound}" when {
      "no users are assigned to the enrolment" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Right(QueryUsersHttpParser.NoUsersFound))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.NoUsersFound)
      }
    }

    s"return ${AutoEnrolmentService.EnrolmentStoreProxyConnectionFailure}" when {
      "an error response is returned when getting users assigned to enrolment" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Left(QueryUsersHttpParser.EnrolmentStoreProxyConnectionFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.EnrolmentStoreProxyConnectionFailure)
      }
    }

    s"return ${AutoEnrolmentService.NoAdminUsers}" when {
      "none of the users assigned to the enrolment are admins" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Right(QueryUsersHttpParser.UsersFound(Set(testUserId1, testUserId2))))
        mockGetUsersForGroups(testGroupId)(Right(GetUsersForGroupHttpParser.UsersFound(Map[String, CredentialRole](
          testUserId1 -> Assistant,
          testUserId2 -> Assistant
        ))))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.NoAdminUsers)
      }
    }

    s"return ${AutoEnrolmentService.UsersGroupSearchFailure}" when {
      "users group search returns an error response" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Right(QueryUsersHttpParser.UsersFound(Set(testUserId1, testUserId2))))
        mockGetUsersForGroups(testGroupId)(Left(GetUsersForGroupHttpParser.UsersGroupsSearchConnectionFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.UsersGroupSearchFailure)
      }
    }

    s"return ${AutoEnrolmentService.UpsertEnrolmentFailure("error message")}" when {
      "upserting enrolment failed" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Right(QueryUsersHttpParser.UsersFound(Set(testUserId1, testUserId2))))
        mockGetUsersForGroups(testGroupId)(Right(GetUsersForGroupHttpParser.UsersFound(Map[String, CredentialRole](
          testUserId1 -> User,
          testUserId2 -> Assistant
        ))))
        mockEnrolmentStoreUpsertEnrolment(testMtditid, testNino)(Left(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentFailure(
          INTERNAL_SERVER_ERROR, "error message"
        )))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.UpsertEnrolmentFailure("error message"))
      }
    }

    s"return ${AutoEnrolmentService.EnrolAdminIdFailure(testUserId1, "error message")}" when {
      "the enrolment failed to be allocated to the group" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Right(QueryUsersHttpParser.UsersFound(Set(testUserId1, testUserId2))))
        mockGetUsersForGroups(testGroupId)(Right(GetUsersForGroupHttpParser.UsersFound(Map[String, CredentialRole](
          testUserId1 -> User,
          testUserId2 -> Assistant
        ))))
        mockEnrolmentStoreUpsertEnrolment(testMtditid, testNino)(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess))
        mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testUserId1, testMtditid)(Left(AllocateEnrolmentResponseHttpParser.EnrolFailure("error message")))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.EnrolAdminIdFailure(testUserId1, "error message"))
      }
    }

    s"return ${AutoEnrolmentService.EnrolmentAssignmentFailureForIds(Set(testUserId2))}" when {
      s"failed to assign $testUserId2 to the enrolment" in {
        mockGetGroupIdForEnrolment(testSAEnrolment)(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        mockGetUserIds(testUtr)(Right(QueryUsersHttpParser.UsersFound(Set(testUserId1, testUserId2))))
        mockGetUsersForGroups(testGroupId)(Right(GetUsersForGroupHttpParser.UsersFound(Map[String, CredentialRole](
          testUserId1 -> User,
          testUserId2 -> Assistant
        ))))
        mockEnrolmentStoreUpsertEnrolment(testMtditid, testNino)(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess))
        mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testUserId1, testMtditid)(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess))
        mockAssignEnrolment(Set(testUserId2), testMtditid)(Left(AssignEnrolmentToUserService.EnrolmentAssignmentFailed(Set(testUserId2))))

        val result: Future[AutoClaimEnrolmentResponse] = TestAutoEnrolmentService.autoClaimEnrolment(utr = testUtr, nino = testNino, mtditid = testMtditid)

        await(result) mustBe Left(AutoEnrolmentService.EnrolmentAssignmentFailureForIds(Set(testUserId2)))
      }
    }
  }

}
