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

package incometax.subscription.services

import core.config.featureswitch.FeatureSwitching
import core.utils.TestConstants._
import core.utils.TestModels._
import core.utils.UnitTestTrait
import incometax.subscription.services.mocks.TestSubscriptionStoreService
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class SubscriptionStoreServiceSpec extends UnitTestTrait with TestSubscriptionStoreService with FeatureSwitching {
  "retrieveStoredSubscription" when {
    "the unauthorised agent feature switch is on" when {
      "the subscription store connector returns a successful stored submission" should {
        "store the submission in keystore and return it" in {
          mockRetrieveSubscriptionData(testNino)(successfulRetrieveSubscriptionResponse)
          setupMockKeystoreSaveFunctions()

          val res = await(TestSubscriptionStoreService.retrieveSubscriptionData(testNino))

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

          val res = await(TestSubscriptionStoreService.retrieveSubscriptionData(testNino))

          res mustBe empty
        }
      }

      "the subscription store connector fails" should {
        "return future failed" in {
          mockRetrieveSubscriptionData(testNino)(retrieveSubscriptionFailure)

          intercept[InternalServerException](await(TestSubscriptionStoreService.retrieveSubscriptionData(testNino)))
        }
      }
    }
    "the unauthorised agent feature switch is off" should {
      "return Future(None)" in {
        val res = await(TestSubscriptionStoreServiceDisabled.retrieveSubscriptionData(testNino))

        res mustBe empty
      }
    }
  }
}
