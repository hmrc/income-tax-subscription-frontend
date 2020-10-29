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
import models.common.IncomeSourceModel
import models.{AgentSummary, IndividualSummary}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Matchers._
import utilities.SubscriptionDataUtil._
import utilities.TestModels._

class SubscriptionDataUtilSpec extends UnitTestTrait
  with FeatureSwitching
  with BeforeAndAfterEach {

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getIncomeSource shouldBe None
      emptyCacheMap.getIncomeSource shouldBe None
      emptyCacheMap.getBusinessName shouldBe None
      emptyCacheMap.getSelectedTaxYear shouldBe None
      emptyCacheMap.getAccountingMethod shouldBe None
      emptyCacheMap.getPropertyAccountingMethod shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getIncomeSource shouldBe Some(IncomeSourceModel(true, true, false))
      testCacheMap.getBusinessName shouldBe Some(testBusinessName)
      testCacheMap.getAccountingMethod shouldBe Some(testAccountingMethod)
      testCacheMap.getSelectedTaxYear shouldBe Some(testSelectedTaxYearNext)
      testCacheMap.getPropertyAccountingMethod.contains(testAccountingMethodProperty) shouldBe true
      testCacheMap.getPropertyAccountingMethod shouldBe Some(testAccountingMethodProperty)
    }

    "The getSummary should populate the Summary model correctly" when {
      "income source is just uk property" in {
        testCacheMapCustom(incomeSource = testIncomeSourceProperty).getSummary() shouldBe
          IndividualSummary(
            incomeSource = testIncomeSourceProperty,
            accountingMethodProperty = testAccountingMethodProperty
          )
      }
      "income source is just business" in {
        testCacheMapCustom(incomeSource = testIncomeSourceBusiness).getSummary() shouldBe
          IndividualSummary(
            incomeSource = testIncomeSourceBusiness,
            businessName = testBusinessName,
            selectedTaxYear = testSelectedTaxYearNext,
            accountingMethod = testAccountingMethod
          )
      }
      "income source is only foreign property" in {
        testCacheMapCustom(incomeSource = testIncomeSourceOverseasProperty).getSummary() shouldBe
          IndividualSummary(
            incomeSource = testIncomeSourceOverseasProperty,
            overseasAccountingMethodProperty = testOverseasAccountingMethodProperty
          )
      }
      "income source is all property and business" in {
        testCacheMapCustom(incomeSource = testIncomeSourceAll).getSummary() shouldBe
          IndividualSummary(
            incomeSource = testIncomeSourceAll,
            businessName = testBusinessName,
            accountingMethod = testAccountingMethod,
            accountingMethodProperty = testAccountingMethodProperty,
            overseasAccountingMethodProperty = testOverseasAccountingMethodProperty
          )
      }
      "income source is neither property or business" in {
        emptyCacheMap.getSummary() shouldBe IndividualSummary()
      }
    }

    "The getAgentSummary should populate the Summary model correctly" when {
      "the income type is property" in {
        testCacheMapCustom(
          incomeSource = Some(testAgentIncomeSourceProperty)
        ).getAgentSummary() shouldBe AgentSummary(
          incomeSource = Some(testAgentIncomeSourceProperty),
          businessName = None,
          accountingMethod = None,
          accountingMethodProperty = Some(testAccountingMethodProperty)
        )
      }

      "the income type is business" in {
        testCacheMapCustom(
          incomeSource = Some(testAgentIncomeSourceBusiness)
        ).getAgentSummary() shouldBe AgentSummary(
          incomeSource = Some(testAgentIncomeSourceBusiness),
          selectedTaxYear = Some(testSelectedTaxYearNext),
          businessName = Some(testBusinessName),
          accountingMethod = Some(testAccountingMethod),
          accountingMethodProperty = None
        )
      }

      "the income type is both" in {
        testCacheMapCustom(
          incomeSource = Some(testAgentIncomeSourceBoth)
        ).getAgentSummary() shouldBe
          AgentSummary(
            incomeSource = testAgentIncomeSourceBoth,
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
