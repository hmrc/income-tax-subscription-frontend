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

package incometax.subscription.services

import core.Constants
import core.Constants.GovernmentGateway._
import incometax.subscription.models.{EnrolmentKey, EnrolmentVerifiers, KnownFactsFailure, KnownFactsSuccess}
import incometax.subscription.services.mocks.TestKnownFactsService
import org.scalatest.concurrent.ScalaFutures
import core.utils.TestConstants._
import core.utils.UnitTestTrait

import scala.concurrent.Future

class KnownFactsServiceSpec extends UnitTestTrait with TestKnownFactsService with ScalaFutures {

  "addKnownFacts" should {

    def result: Future[Either[KnownFactsFailure, KnownFactsSuccess.type]] =
      TestKnownFactsService.addKnownFacts(testMTDID, testNino)

      val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMTDID)
      val testEnrolmentVerifiers = EnrolmentVerifiers(NINO -> testNino)

    "return a success from the EnrolmentStoreConnector" in {
      mockUpsertEnrolmentSuccess(testEnrolmentKey, testEnrolmentVerifiers)

      whenReady(result)(_ mustBe Right(KnownFactsSuccess))
    }

    "return a failure from the EnrolmentStoreConnector" in {
      mockUpsertEnrolmentFailure(testEnrolmentKey, testEnrolmentVerifiers)

      whenReady(result)(_ mustBe Left(KnownFactsFailure(testErrorMessage)))
    }

    "pass through the exception if the EnrolmentStoreConnector fails" in {
      mockUpsertEnrolmentException(testEnrolmentKey, testEnrolmentVerifiers)

      whenReady(result.failed)(_ mustBe testException)
    }
  }


}
