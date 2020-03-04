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

package services

import connectors.agent.httpparsers.AssignEnrolmentToUserHttpParser.{EnrolmentAssigned, EnrolmentAssignmentFailure}
import connectors.agent.mocks.MockEnrolmentStoreProxyConnector
import org.scalatest.{Matchers, WordSpec}
import play.api.test.Helpers._
import services.AssignEnrolmentToUserService.{EnrolmentAssignedToUsers, EnrolmentAssignmentFailed}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AssignEnrolmentToUserServiceSpec extends WordSpec with Matchers with MockEnrolmentStoreProxyConnector {

  object TestAssignEnrolmentToUserService extends AssignEnrolmentToUserService(mockEnrolmentStoreProxyConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testMtditid: String = "XAIT00000000000"

  "assignEnrolment" should {
    "return Right(EnrolmentsAssigned)" when {
      "the connector returns that all the users have been assigned the enrolment" in {
        val testUserIdSet: Set[String] = Set("userIdOne", "userIdTwo")

        testUserIdSet foreach (userId => mockAssignEnrolment(userId, testMtditid)(Right(EnrolmentAssigned)))

        val res = await(TestAssignEnrolmentToUserService.assignEnrolment(testUserIdSet, testMtditid))

        res shouldBe Right(EnrolmentAssignedToUsers)
      }
    }

    "return a Left(EnrolmentAssignmentFailed)" when {
      "the connector returns that it failed to assign some of the enrolments" in {
        val testUserIdSet = Set("userIdOne", "userIdTwo")

        mockAssignEnrolment("userIdOne", testMtditid)(Right(EnrolmentAssigned))
        mockAssignEnrolment("userIdTwo", testMtditid)(Left(EnrolmentAssignmentFailure(BAD_REQUEST, "")))

        val res = await(TestAssignEnrolmentToUserService.assignEnrolment(testUserIdSet, testMtditid))

        res shouldBe Left(EnrolmentAssignmentFailed(Set("userIdTwo")))
      }
    }
  }

}
