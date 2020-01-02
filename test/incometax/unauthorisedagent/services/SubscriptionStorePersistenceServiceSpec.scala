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

import agent.utils.TestConstants._
import agent.utils.TestModels._
import core.utils.UnitTestTrait
import incometax.subscription.models.IncomeSourceType
import incometax.unauthorisedagent.models.{StoreSubscriptionFailure, StoreSubscriptionSuccess, StoredSubscription}
import incometax.unauthorisedagent.services.mocks.TestSubscriptionStorePersistenceService
import play.api.test.Helpers._

class SubscriptionStorePersistenceServiceSpec extends UnitTestTrait with TestSubscriptionStorePersistenceService {

  val testPropertyKeystoreCache =
    testCacheMap(
      incomeSource = testIncomeSourceProperty,
      otherIncome = testOtherIncomeYes,
      accountingPeriodDate = None,
      businessName = None,
      accountingMethod = None
    )

  val testPropertyExpectedStoredSubscription =
    StoredSubscription(
      arn = testARN,
      incomeSource = IncomeSourceType(testIncomeSourceProperty.source),
      otherIncome = true
    )

  "getSubscriptionData" should {
    "fetch property only data from keystore and convert it to the StoredSubscription model" in {
      setupMockKeystore(fetchAll = testPropertyKeystoreCache)

      val storeSubscription = TestSubscriptionStorePersistenceService.getSubscriptionData(testARN)

      await(storeSubscription) mustBe testPropertyExpectedStoredSubscription
    }

    "fetch business only data from keystore and convert it to the StoredSubscription model" in {
      setupMockKeystore(
        fetchAll =
          testCacheMap(
            incomeSource = testIncomeSourceBusiness,
            otherIncome = testOtherIncomeNo,
            accountingPeriodDate = testAccountingPeriod,
            businessName = testBusinessName,
            accountingMethod = testAccountingMethod
          )
      )

      val expected = StoredSubscription(
        arn = testARN,
        incomeSource = IncomeSourceType(testIncomeSourceBusiness.source),
        otherIncome = false,
        accountingPeriodStart = testStartDate,
        accountingPeriodEnd = testEndDate,
        tradingName = testBusinessName.businessName,
        cashOrAccruals = testAccountingMethod.accountingMethod
      )
      val storeSubscription = TestSubscriptionStorePersistenceService.getSubscriptionData(testARN)
      await(storeSubscription) mustBe expected
    }

    "fetch business and property data from keystore and convert it to the StoredSubscription model" in {
      setupMockKeystore(
        fetchAll =
          testCacheMap(
            incomeSource = testIncomeSourceBoth,
            otherIncome = testOtherIncomeNo,
            accountingPeriodDate = testAccountingPeriod,
            businessName = testBusinessName,
            accountingMethod = testAccountingMethod
          )
      )

      val expected = StoredSubscription(
        arn = testARN,
        incomeSource = IncomeSourceType(testIncomeSourceBoth.source),
        otherIncome = false,
        accountingPeriodStart = testStartDate,
        accountingPeriodEnd = testEndDate,
        tradingName = testBusinessName.businessName,
        cashOrAccruals = testAccountingMethod.accountingMethod
      )
      val storeSubscription = TestSubscriptionStorePersistenceService.getSubscriptionData(testARN)
      await(storeSubscription) mustBe expected
    }
  }

  "storeSubscription" should {
    "if calling store is successful then return StoreSubscriptionSuccess" in {
      setupMockKeystore(fetchAll = testPropertyKeystoreCache)

      mockStoreSubscriptionDataSuccess(testNino, testPropertyExpectedStoredSubscription)

      val storeSubscription = TestSubscriptionStorePersistenceService.storeSubscription(testARN, testNino)
      await(storeSubscription) mustBe Right(StoreSubscriptionSuccess)
    }

    "if calling store is a failure then return StoreSubscriptionFailure" in {
      setupMockKeystore(fetchAll = testPropertyKeystoreCache)

      mockStoreSubscriptionDataFailure(testNino, testPropertyExpectedStoredSubscription)

      val storeSubscription = TestSubscriptionStorePersistenceService.storeSubscription(testARN, testNino)
      await(storeSubscription) mustBe Left(StoreSubscriptionFailure(testErrorMessage))
    }
  }

}
