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

package core.services

import core.utils.UnitTestTrait
import incometax.subscription.models.SummaryModel
import org.scalatest.Matchers._

class CacheUtilSpec extends UnitTestTrait {

  import core.services.CacheUtil._
  import core.utils.TestModels._

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getIncomeSource() shouldBe None
      emptyCacheMap.getOtherIncome() shouldBe None
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getBusinessPhoneNumber() shouldBe None
      emptyCacheMap.getBusinessAddress() shouldBe None
      emptyCacheMap.getBusinessStartDate() shouldBe None
      emptyCacheMap.getMatchTaxYear() shouldBe None
      emptyCacheMap.getAccountingPeriodDate() shouldBe None
      emptyCacheMap.getAccountingMethod() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getIncomeSource() shouldBe Some(testIncomeSourceBoth)
      testCacheMap.getOtherIncome() shouldBe Some(testOtherIncomeNo)
      testCacheMap.getBusinessName() shouldBe Some(testBusinessName)
      testCacheMap.getBusinessPhoneNumber() shouldBe Some(testBusinessPhoneNumber)
      testCacheMap.getBusinessAddress() shouldBe Some(testAddress)
      testCacheMap.getBusinessStartDate() shouldBe Some(testBusinessStartDate)
      testCacheMap.getMatchTaxYear() shouldBe Some(testMatchTaxYearNo)
      testCacheMap.getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingMethod() shouldBe Some(testAccountingMethod)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "The getSummary should populate the Summary model correctly" in {
      testCacheMap.getSummary() shouldBe
        SummaryModel(
          incomeSource = testIncomeSourceBoth,
          otherIncome = testOtherIncomeNo,
          matchTaxYear = testMatchTaxYearNo,
          accountingPeriod = testAccountingPeriod,
          businessName = testBusinessName,
          businessPhoneNumber = testBusinessPhoneNumber,
          businessAddress = testAddress,
          businessStartDate = testBusinessStartDate,
          accountingMethod = testAccountingMethod,
          terms = testTerms
        )

      // for the property only journey, this should only populate the subset of views
      // relevant to the journey
      val overPopulatedPropertyCacheMap =
      testCacheMap(
        incomeSource = testIncomeSourceProperty,
        otherIncome = testOtherIncomeNo,
        matchTaxYear = testMatchTaxYearNo,
        accountingPeriodDate = testAccountingPeriod,
        businessName = testBusinessName,
        businessPhoneNumber = testBusinessPhoneNumber,
        businessAddress = testAddress,
        businessStartDate = testBusinessStartDate,
        accountingMethod = testAccountingMethod,
        terms = testTerms,
        accountingPeriodPrior = None // no longer used in individual journey
      )
      overPopulatedPropertyCacheMap.getSummary() shouldBe
        SummaryModel(
          testIncomeSourceProperty,
          testOtherIncomeNo,
          terms = testTerms
        )

      emptyCacheMap.getSummary() shouldBe SummaryModel()
    }

  }
}

