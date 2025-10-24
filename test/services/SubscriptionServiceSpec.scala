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

import connectors.httpparser.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import models.common.subscription._
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers._
import play.api.test.Helpers._
import services.mocks.TestSubscriptionService
import utilities.individual.TestConstants
import utilities.individual.TestConstants._

class SubscriptionServiceSpec extends TestSubscriptionService
  with EitherValues {

  val testNino: String = TestConstants.testNino
  val testTaxYear: String = "2023-24"

  "SubscriptionService.getSubscription" should {

    def call: GetSubscriptionResponse = await(TestSubscriptionService.getSubscription(nino = testNino))

    "return the safeId when the subscription is returned" in {
      setupMockGetSubscriptionFound(testNino)
      call.value shouldBe Some(SubscriptionSuccess(testMTDID))
    }

    "return the None when the subscription is returned as None" in {
      setupMockGetSubscriptionNotFound(testNino)
      call.value shouldBe None
    }

    "return the error if subscription fails on bad request" in {
      setupMockGetSubscriptionFailure(testNino)
      call.left.value shouldBe SubscriptionFailureResponse(BAD_REQUEST)
    }

    "return the error if subscription throws an exception" in {
      setupMockGetSubscriptionException(testNino)
      intercept[Exception](call) shouldBe testException
    }
  }

}
