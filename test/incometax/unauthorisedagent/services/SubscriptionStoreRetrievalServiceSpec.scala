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

package incometax.unauthorisedagent.services

import core.config.featureswitch.FeatureSwitching
import core.utils.TestConstants._
import core.utils.TestModels._
import core.utils.UnitTestTrait
import incometax.unauthorisedagent.models.DeleteSubscriptionSuccess
import incometax.unauthorisedagent.services.mocks.TestSubscriptionStoreRetrievalService
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class SubscriptionStoreRetrievalServiceSpec extends UnitTestTrait with TestSubscriptionStoreRetrievalService with FeatureSwitching {
  "retrieveStoredSubscription" when {
    "the unauthorised agent feature switch is on" when {
      "the subscription store connector returns a successful stored submission" should {
        "store the submission in keystore and return it" in {
          mockRetrieveSubscriptionData(testNino)(successfulRetrieveSubscriptionResponse)
          setupMockKeystoreSaveFunctions()

          val res = await(TestSubscriptionStoreRetrievalService.retrieveSubscriptionData(testNino))

          res must contain(testStoredSubscription)
          verifyKeystore(
            saveIncomeSource = Some(1),
            saveOtherIncome = Some(1),
            saveAccountingPeriodDate = Some(1),
            saveBusinessName = Some(1),
            saveAccountingMethod = Some(1)
          )
        }
      }
      "the subscription store connector returns a successful empty submission" should {
        "return Future(None)" in {
          mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)

          val res = await(TestSubscriptionStoreRetrievalService.retrieveSubscriptionData(testNino))

          res mustBe empty
        }
      }

      "the subscription store connector fails" should {
        "return future failed" in {
          mockRetrieveSubscriptionData(testNino)(retrieveSubscriptionFailure)

          intercept[InternalServerException](await(TestSubscriptionStoreRetrievalService.retrieveSubscriptionData(testNino)))
        }
      }
    }
    "the unauthorised agent feature switch is off" should {
      "return Future(None)" in {
        val res = await(TestSubscriptionStoreRetrievalServiceDisabled.retrieveSubscriptionData(testNino))

        res mustBe empty
      }
    }
  }

  "deleteStoredSubscription" when {
    "the subscription store connector returns a success" should {
      "return a success" in {
        mockDeleteSubscriptionData(testNino)(deleteSubscriptionSuccess)

        val res = await(TestSubscriptionStoreRetrievalServiceDisabled.deleteSubscriptionData(testNino))

        res mustBe DeleteSubscriptionSuccess
      }
    }
    "the subscription store connector returns a failure" should {
      "throw an exception" in {
        mockDeleteSubscriptionData(testNino)(deleteSubscriptionFailure)

        intercept[InternalServerException](await(TestSubscriptionStoreRetrievalService.deleteSubscriptionData(testNino)))
      }
    }
  }
}
