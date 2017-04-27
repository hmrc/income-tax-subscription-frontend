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

package connectors

import connectors.mocks.MockSubscriptionConnector
import connectors.models.subscription.{FEResponse, FESuccessResponse}
import org.scalatest.Matchers._
import play.api.test.Helpers._
import utils.TestConstants

class SubscriptionConnectorSpec extends MockSubscriptionConnector {

  val nino: String = TestConstants.testNino
  val id: String = TestConstants.testMTDID

  "SubscriptionConnector.subscribe" should {

    "Post to the correct url" in {
      TestSubscriptionConnector.subscriptionUrl(TestConstants.testNino) should endWith(s"/income-tax-subscription/subscription/${TestConstants.testNino}")
    }

    def call = await(TestSubscriptionConnector.subscribe(request = testRequest))

    "return the succcess response as an object" in {
      setupSubscribe(testRequest)(subscribeSuccess)
      val expected = FESuccessResponse(id)
      val actual = call
      actual shouldBe Some(expected)
    }

    "return None if the middle service indicated a bad request" in {
      val reason = "Your submission contains one or more errors. Failed Parameter(s) - [idType, idNumber, payload]"
      val code = "INVALID_NINO"
      setupSubscribe(testRequest)(subscribeBadRequest)
      val actual = call
      actual shouldBe None
    }

    "return None if the middle service indicated internal server error" in {
      setupSubscribe(testRequest)(subscribeInternalServerError)
      val actual = call
      actual shouldBe None
    }
  }

  "SubscriptionConnector.getSubscription" should {

    val testNino = TestConstants.testNino
    "GET to the correct url" in {
      TestSubscriptionConnector.subscriptionUrl(testNino) should endWith(s"/income-tax-subscription/subscription/$testNino")
    }

    def result: Option[FEResponse] = await(TestSubscriptionConnector.getSubscription(testNino))

    "return the succcess response as an object" in {
      setupGetSubscription(testNino)(subscribeSuccess)
      result shouldBe Some(FESuccessResponse(id))
    }

    "return the None response as an object" in {
      setupGetSubscription(testNino)(subscribeNone)
      result shouldBe Some(FESuccessResponse(None))
    }

    "return fail if the middle service indicated a bad request" in {
      setupGetSubscription(testNino)(subscribeBadRequest)
      result shouldBe None
    }

    "return None if the middle service indicated internal server error" in {
      setupGetSubscription(testNino)(subscribeInternalServerError)
      result shouldBe None
    }
  }

}
