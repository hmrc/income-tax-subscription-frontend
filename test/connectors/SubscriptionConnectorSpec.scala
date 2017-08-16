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

import connectors.mocks.TestSubscriptionConnector
import connectors.models.subscription.SubscriptionResponse.{BadlyFormattedSubscriptionResponse, SubscriptionFailureResponse, SubscriptionResponse, SubscriptionSuccess}
import org.scalatest.Matchers._
import org.scalatest.{EitherValues, OptionValues}
import play.api.test.Helpers._
import utils.TestConstants._

class SubscriptionConnectorSpec extends TestSubscriptionConnector with EitherValues with OptionValues {
  "SubscriptionConnector.subscribe" should {

    "Post to the correct url" in {
      TestSubscriptionConnector.subscriptionUrl(testNino) should endWith(s"/income-tax-subscription/subscription/$testNino")
    }

//    def call: SubscriptionResponse = await(TestSubscriptionConnector.subscribe(request = testSubmissionRequest))
//
//    "return the success response as an object" in {
//      setupMockSubscribeSuccess(testSubmissionRequest)
//
//      call.right.value shouldBe SubscriptionSuccess(testMTDID)
//    }
//
//    "return an error if the response is badly formatted" in {
//      setupMockSubscribeEmptyBody(testSubmissionRequest)
//      call.left.value shouldBe BadlyFormattedSubscriptionResponse
//    }
//
//    "return the correct error if the middle service indicated a bad request" in {
//      setupMockSubscribeBadRequest(testSubmissionRequest)
//      call.left.value shouldBe SubscriptionFailureResponse(BAD_REQUEST)
//    }
  }

  "SubscriptionConnector.getSubscription" should {

    "GET to the correct url" in {
      TestSubscriptionConnector.subscriptionUrl(testNino) should endWith(s"/income-tax-subscription/subscription/$testNino")
    }

//    def result: Either[SubscriptionFailureResponse, Option[SubscriptionSuccess]] = await(TestSubscriptionConnector.getSubscription(testNino))
//
//    "return the success response as an object" in {
//      setupMockGetSubscriptionSuccess(testNino)
//      result.right.value shouldBe Some(SubscriptionSuccess(testMTDID))
//    }
//
//    "return an empty OK response as None" in {
//      setupMockGetSubscriptionEmptyBody(testNino)
//      result.right.value shouldBe empty
//    }
//
//    "return fail if the middle service indicated a bad request" in {
//      setupMockGetSubscriptionBadRequest(testNino)
//      result.left.value shouldBe SubscriptionFailureResponse(BAD_REQUEST)
//    }
  }

}
