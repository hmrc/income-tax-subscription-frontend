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

package services

import common.Constants.{utrEnrolmentIdentifierKey, utrEnrolmentName}
import connectors.agent.httpparsers.EnrolmentStoreProxyHttpParser
import connectors.agent.mocks.MockEnrolmentStoreProxyConnector
import models.common.subscription.EnrolmentKey
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.*
import services.agent.{CheckEnrolmentAllocationService, CheckEnrolmentAllocationServiceModel}
import uk.gov.hmrc.domain.*
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class CheckEnrolmentAllocationServiceSpec extends AnyWordSpec with Matchers
  with MockEnrolmentStoreProxyConnector {

  object TestCheckEnrolmentAllocationService extends CheckEnrolmentAllocationService(
    mockEnrolmentStoreProxyConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getGroupIdForEnrolment" when {
    "EnrolmentStoreProxy returns EnrolmentNotAllocated" should {
      "returns EnrolmentNotAllocated" in {
        lazy val testUtr: String = new AtedUtrGenerator().nextAtedUtr.utr
        lazy val testEnrolment: EnrolmentKey = EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr)
        mockGetAllocatedEnrolment(testEnrolment)(Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForEnrolment(testEnrolment)

        await(res) shouldBe Right(CheckEnrolmentAllocationServiceModel.EnrolmentNotAllocated)
      }
    }
    "EnrolmentStoreProxy returns EnrolmentAlreadyAllocated" should {
      "return EnrolmentAlreadyAllocated" in {
        lazy val testUtr: String = new AtedUtrGenerator().nextAtedUtr.utr
        lazy val testEnrolment: EnrolmentKey = EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr)
        val testGroupId: String = UUID.randomUUID.toString
        mockGetAllocatedEnrolment(testEnrolment)(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId)))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForEnrolment(testEnrolment)

        await(res) shouldBe Left(CheckEnrolmentAllocationServiceModel.EnrolmentAlreadyAllocated(testGroupId))
      }
    }
    "EnrolmentStoreProxy returns an unexpected failure" should {
      "return Failure" in {
        lazy val testUtr: String = new AtedUtrGenerator().nextAtedUtr.utr
        lazy val testEnrolment: EnrolmentKey = EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr)
        mockGetAllocatedEnrolment(testEnrolment)(Left(EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyFailure(BAD_REQUEST)))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForEnrolment(testEnrolment)

        await(res) shouldBe Left(CheckEnrolmentAllocationServiceModel.UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST))
      }
    }
    "EnrolmentStoreProxy returns invalid json" should {
      "return InvalidJson response" in {
        lazy val testUtr: String = new AtedUtrGenerator().nextAtedUtr.utr
        lazy val testEnrolment: EnrolmentKey = EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr)
        mockGetAllocatedEnrolment(testEnrolment)(Left(EnrolmentStoreProxyHttpParser.InvalidJsonResponse))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForEnrolment(testEnrolment)

        await(res) shouldBe Left(CheckEnrolmentAllocationServiceModel.EnrolmentStoreProxyInvalidJsonResponse)
      }
    }
  }
}


