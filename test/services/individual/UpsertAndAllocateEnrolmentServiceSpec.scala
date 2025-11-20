/*
 * Copyright 2025 HM Revenue & Customs
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

package services.individual

import auth.MockAuth
import common.Constants
import connectors.individual.subscription.mocks.MockTaxEnrolmentsConnector
import models.common.subscription.{EmacEnrolmentRequest, EnrolmentKey, EnrolmentVerifiers}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class UpsertAndAllocateEnrolmentServiceSpec extends PlaySpec with MockTaxEnrolmentsConnector with MockAuth {

  "upsertAndAllocate" must {
    "return an UpsertAndAllocateEnrolmentSuccess" when {
      "the enrolment was successfully upserted and allocated" in {
        mockUpsertEnrolmentSuccess(testEnrolmentKey, testEnrolmentVerifiers)
        mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(Some(testCredentials), Some(testGroupId)))
        mockAllocateEnrolmentSuccess(testGroupId, testEnrolmentKey, testEnrolmentRequest)

        val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino)

        await(result) mustBe Right(UpsertAndAllocateEnrolmentService.UpsertAndAllocateEnrolmentSuccess)

        verifyUpsertEnrolment(testEnrolmentKey, testEnrolmentVerifiers)
        verifyAllocateEnrolment(testGroupId, testEnrolmentKey, testEnrolmentRequest)
      }
    }
    "return an UpsertAndAllocateEnrolmentFailure" which {
      "is an UpsertKnownFactsFailure" when {
        "there is a failure upserting known facts" in {
          mockUpsertEnrolmentFailure(testEnrolmentKey, testEnrolmentVerifiers)

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.UpsertKnownFactsFailure)

          verifyUpsertEnrolment(testEnrolmentKey, testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey, testEnrolmentRequest, count = 0)
        }
      }
      "is a NoGroupIdFailure" when {
        "there is no group id returned from auth" in {
          mockUpsertEnrolmentSuccess(testEnrolmentKey, testEnrolmentVerifiers)
          mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(Some(testCredentials), None))

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.NoGroupIdFailure)

          verifyUpsertEnrolment(testEnrolmentKey, testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey, testEnrolmentRequest, count = 0)
        }
      }
      "is a NoCredentialsFailure" when {
        "there is no credential returned from auth" in {
          mockUpsertEnrolmentSuccess(testEnrolmentKey, testEnrolmentVerifiers)
          mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(None, Some(testGroupId)))

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.NoCredentialsFailure)

          verifyUpsertEnrolment(testEnrolmentKey, testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey, testEnrolmentRequest, count = 0)
        }
      }
      "is an AllocateEnrolmentFailure" when {
        "there is a failure allocating the enrolment" in {
          mockUpsertEnrolmentSuccess(testEnrolmentKey, testEnrolmentVerifiers)
          mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(Some(testCredentials), Some(testGroupId)))
          mockAllocateEnrolmentFailure(testGroupId, testEnrolmentKey, testEnrolmentRequest)

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.AllocateEnrolmentFailure)

          verifyUpsertEnrolment(testEnrolmentKey, testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey, testEnrolmentRequest)
        }
      }
    }
  }

  object TestUpsertAndAllocateEnrolmentService extends UpsertAndAllocateEnrolmentService(
    mockTaxEnrolmentsConnector, mockAuth
  )

  lazy val testMTDITID: String = "test-mtditid"
  lazy val testNino: String = "test-nino"
  lazy val testEnrolmentKey: EnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, Constants.mtdItsaEnrolmentIdentifierKey -> testMTDITID)
  lazy val testEnrolmentVerifiers: EnrolmentVerifiers = EnrolmentVerifiers(Constants.ninoEnrolmentIdentifierKey -> testNino)
  lazy val testCredentialId: String = "test-cred-id"
  lazy val testEnrolmentRequest: EmacEnrolmentRequest = EmacEnrolmentRequest(testCredentialId, testNino)
  lazy val testCredentials: Credentials = Credentials(testCredentialId, "")
  lazy val testGroupId: String = "test-group-id"

  lazy implicit val hc: HeaderCarrier = HeaderCarrier()

}
