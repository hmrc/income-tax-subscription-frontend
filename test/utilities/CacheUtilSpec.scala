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

package utilities

import config.featureswitch.FeatureSwitching
import models.individual.subscription.{AgentSummary, Both, IndividualSummary}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Matchers._
import utilities.AccountingPeriodUtil.getCurrentTaxYear
import utilities.TestModels._
import CacheUtil._

class CacheUtilSpec extends UnitTestTrait
  with FeatureSwitching
  with BeforeAndAfterEach {

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getRentUkProperty shouldBe None
      emptyCacheMap.getAreYouSelfEmployed shouldBe None
      emptyCacheMap.getIncomeSourceType shouldBe None
      emptyCacheMap.agentGetIncomeSource shouldBe None
      emptyCacheMap.getBusinessName shouldBe None
      emptyCacheMap.getBusinessPhoneNumber shouldBe None
      emptyCacheMap.getBusinessAddress shouldBe None
      emptyCacheMap.getBusinessStartDate shouldBe None
      emptyCacheMap.getMatchTaxYear shouldBe None
      emptyCacheMap.getEnteredAccountingPeriodDate shouldBe None
      emptyCacheMap.getSelectedTaxYear shouldBe None
      emptyCacheMap.getAccountingMethod shouldBe None
      emptyCacheMap.getPropertyAccountingMethod shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getRentUkProperty shouldBe Some(testRentUkProperty_property_and_other)
      testCacheMap.getAreYouSelfEmployed shouldBe Some(testAreYouSelfEmployed_yes)
      testCacheMap.getIncomeSourceType shouldBe Some(Both)
      testCacheMap.agentGetIncomeSource shouldBe Some(Both)
      testCacheMap.getBusinessName shouldBe Some(testBusinessName)
      testCacheMap.getBusinessPhoneNumber shouldBe Some(testBusinessPhoneNumber)
      testCacheMap.getBusinessAddress shouldBe Some(testAddress)
      testCacheMap.getBusinessStartDate shouldBe Some(testBusinessStartDate)
      testCacheMap.getMatchTaxYear shouldBe Some(testMatchTaxYearNo)
      testCacheMap.getEnteredAccountingPeriodDate shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingMethod shouldBe Some(testAccountingMethod)
      testCacheMap.getSelectedTaxYear shouldBe Some(testSelectedTaxYearNext)
      testCacheMap.getPropertyAccountingMethod.contains(testAccountingMethodProperty) shouldBe true
      testCacheMap.getPropertyAccountingMethod shouldBe Some(testAccountingMethodProperty)
    }

    "The getSummary should populate the Summary model correctly" when {
      "income source is just property" in {
        testCacheMapCustom(rentUkProperty = testRentUkProperty_property_only, areYouSelfEmployed = None).getSummary() shouldBe
          IndividualSummary(
            rentUkProperty = testRentUkProperty_property_only,
            areYouSelfEmployed = None,
            accountingMethodProperty = testAccountingMethodProperty
          )
      }
      "income source is just business" in {
        testCacheMapCustom(rentUkProperty = testRentUkProperty_no_property, areYouSelfEmployed = testAreYouSelfEmployed_yes).getSummary() shouldBe
          IndividualSummary(
            rentUkProperty = testRentUkProperty_no_property,
            areYouSelfEmployed = testAreYouSelfEmployed_yes,
            businessName = testBusinessName,
            businessPhoneNumber = testBusinessPhoneNumber,
            businessAddress = testAddress,
            businessStartDate = testBusinessStartDate,
            selectedTaxYear = testSelectedTaxYearNext,
            accountingMethod = testAccountingMethod
          )
      }
      "income source is both property and business" in {
        testCacheMapCustom(rentUkProperty = testRentUkProperty_property_and_other, areYouSelfEmployed = testAreYouSelfEmployed_yes).getSummary() shouldBe
          IndividualSummary(
            rentUkProperty = testRentUkProperty_property_and_other,
            areYouSelfEmployed = testAreYouSelfEmployed_yes,
            businessName = testBusinessName,
            businessPhoneNumber = testBusinessPhoneNumber,
            businessAddress = testAddress,
            businessStartDate = testBusinessStartDate,
            accountingMethod = testAccountingMethod,
            accountingMethodProperty = testAccountingMethodProperty
          )
      }
      "income source is neither property or business" in {
        emptyCacheMap.getSummary() shouldBe IndividualSummary()
      }
    }

    "The getAgentSummary should populate the Summary model correctly" when {
      "the income type is property" in {
        testCacheMapCustom(
          incomeSource = Some(testIncomeSourceProperty)
        ).getAgentSummary() shouldBe AgentSummary(
          incomeSource = Some(testIncomeSourceProperty),
          matchTaxYear = None,
          accountingPeriodDate = None,
          businessName = None,
          accountingMethod = None,
          accountingMethodProperty = Some(testAccountingMethodProperty)
        )
      }

      "the income type is business" in {
        testCacheMapCustom(
          incomeSource = Some(testIncomeSourceBusiness)
        ).getAgentSummary() shouldBe AgentSummary(
          incomeSource = Some(testIncomeSourceBusiness),
          matchTaxYear = Some(testMatchTaxYearNo),
          selectedTaxYear = Some(testSelectedTaxYearNext),
          accountingPeriodDate = Some(testAccountingPeriod),
          businessName = Some(testBusinessName),
          accountingMethod = Some(testAccountingMethod),
          accountingMethodProperty = None
        )
      }

      "the income type is both" in {
        testCacheMapCustom(
          incomeSource = Some(testIncomeSourceBoth)
        ).getAgentSummary() shouldBe
          AgentSummary(
            incomeSource = testIncomeSourceBoth,
            matchTaxYear = testMatchTaxYearNo,
            accountingPeriodDate = testAccountingPeriod,
            businessName = testBusinessName,
            accountingMethod = testAccountingMethod,
            accountingMethodProperty = testAccountingMethodProperty
          )
      }

      "the income type is not set" in {
        emptyCacheMap.getAgentSummary() shouldBe AgentSummary()
      }
    }
  }

}
