/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.subscription.services

import core.Constants.GovernmentGateway._
import core.utils.TestConstants._
import core.utils.UnitTestTrait
import incometax.subscription.models.{EnrolFailure, EnrolSuccess}
import incometax.subscription.services.mocks.TestEnrolmentService
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}

import scala.concurrent.Future

class EnrolmentServiceSpec extends UnitTestTrait with TestEnrolmentService with ScalaFutures {
  "addKnownFacts" when {
    "the ES8 feature switch is turned off" should {
      def result: Future[Either[EnrolFailure, EnrolSuccess.type]] = TestEnrolmentService.enrol(testMTDID, testNino)

      "return a success from the GGConnector" in {
        mockEnrolSuccess(expectedRequestModel)

        whenReady(result)(_ mustBe Right(EnrolSuccess))
      }

      "return a failure from the GGConnector" in {
        mockEnrolFailure(expectedRequestModel)

        whenReady(result)(_ mustBe Left(EnrolFailure(testErrorMessage)))
      }

      "pass through the exception if the GGConnector fails" in {
        mockEnrolException(expectedRequestModel)

        whenReady(result.failed)(_ mustBe testException)
      }
    }

    "the ES8 feature switch is turned on" should {
      def result: Future[Either[EnrolFailure, EnrolSuccess.type]] = TestEnrolmentServiceFeatureSwitched.enrol(testMTDID, testNino)

      "return a success from the EnrolmentStoreConnector" in {
        mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new ~(Credentials(testCredId, GGProviderId), Some(testGroupId)))
        mockAllocateEnrolmentSuccess(testGroupId, testEnrolmentKey, testEnrolmentRequest)

        whenReady(result)(_ mustBe Right(EnrolSuccess))
      }

      "return a failure from the EnrolmentStoreConnector" in {
        mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new ~(Credentials(testCredId, GGProviderId), Some(testGroupId)))
        mockAllocateEnrolmentFailure(testGroupId, testEnrolmentKey, testEnrolmentRequest)

        whenReady(result)(_ mustBe Left(EnrolFailure(testErrorMessage)))
      }

      "pass through the exception if the EnrolmentStoreConnector fails" in {
        mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new ~(Credentials(testCredId, GGProviderId), Some(testGroupId)))
        mockAllocateEnrolmentException(testGroupId, testEnrolmentKey, testEnrolmentRequest)

        whenReady(result.failed)(_ mustBe testException)
      }
    }
  }
}
