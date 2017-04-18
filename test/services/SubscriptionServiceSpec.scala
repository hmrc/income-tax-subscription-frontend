/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.models.subscription.FESuccessResponse
import org.scalatest.Matchers._
import play.api.test.Helpers._
import services.mocks.MockSubscriptionService
import utils.TestConstants


class SubscriptionServiceSpec extends MockSubscriptionService {


  "SubscriptionService.submitSubscription" should {
    def call = await(TestProtectedMicroserviceConnector.subscribe(request = testRequest))

    "return the safeId when the subscription is successful" in {
      setupSubscribe(subscribeSuccess)
      val response = call.get
      response.isInstanceOf[FESuccessResponse] shouldBe true
      response.asInstanceOf[FESuccessResponse].mtditId shouldBe Some(testId)
    }

    "return the error if subscription fails on bad request" in {
      setupSubscribe(subscribeBadRequest)
      val response = call
      response shouldBe None
    }

    "return the error if subscription fails on internal server error" in {
      setupSubscribe(subscribeInternalServerError)
      val response = call
      response shouldBe None

    }
  }

  "SubscriptionService.getSubscription" should {
    val testNino = TestConstants.testNino

    def call = await(TestProtectedMicroserviceConnector.getSubscription(nino = testNino))

    "return the safeId when the subscription is returned" in {
      setupGetSubscription(testNino)(subscribeSuccess)
      val response = call.get
      response.isInstanceOf[FESuccessResponse] shouldBe true
      response.asInstanceOf[FESuccessResponse].mtditId shouldBe Some(testId)
    }

    "return the None when the subscription is returned as None" in {
      setupGetSubscription(testNino)(subscribeNone)
      val response = call.get
      response.isInstanceOf[FESuccessResponse] shouldBe true
      response.asInstanceOf[FESuccessResponse].mtditId shouldBe None
    }

    "return the error if subscription fails on bad request" in {
      setupGetSubscription(testNino)(subscribeBadRequest)
      val response = call
      response shouldBe None
    }

    "return the error if subscription fails on internal server error" in {
      setupGetSubscription(testNino)(subscribeInternalServerError)
      val response = call
      response shouldBe None
    }
  }

}
