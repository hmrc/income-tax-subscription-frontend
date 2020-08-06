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
import models.individual.incomesource.IncomeSourceModel

class CacheUtilSpec extends UnitTestTrait
  with FeatureSwitching
  with BeforeAndAfterEach {

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getIncomeSourceModel shouldBe None
      emptyCacheMap.agentGetIncomeSource shouldBe None
      emptyCacheMap.getBusinessName shouldBe None
      emptyCacheMap.getMatchTaxYear shouldBe None
      emptyCacheMap.getEnteredAccountingPeriodDate shouldBe None
      emptyCacheMap.getSelectedTaxYear shouldBe None
      emptyCacheMap.getAccountingMethod shouldBe None
      emptyCacheMap.getPropertyAccountingMethod shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getIncomeSourceModel shouldBe Some(IncomeSourceModel(true,true,false))
      testCacheMap.agentGetIncomeSource shouldBe Some(Both)
      testCacheMap.getBusinessName shouldBe Some(testBusinessName)
      testCacheMap.getMatchTaxYear shouldBe Some(testMatchTaxYearNo)
      testCacheMap.getEnteredAccountingPeriodDate shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingMethod shouldBe Some(testAccountingMethod)
      testCacheMap.getSelectedTaxYear shouldBe Some(testSelectedTaxYearNext)
      testCacheMap.getPropertyAccountingMethod.contains(testAccountingMethodProperty) shouldBe true
      testCacheMap.getPropertyAccountingMethod shouldBe Some(testAccountingMethodProperty)
    }

    "The getSummary should populate the Summary model correctly" when {
      "income source is just property" in {
        testCacheMapCustom(incomeSourceIndiv = testIncomeSourceProperty).getSummary() shouldBe
          IndividualSummary(
            incomeSourceIndiv = testIncomeSourceProperty,
            accountingMethodProperty = testAccountingMethodProperty
          )
      }
      "income source is just business" in {
        testCacheMapCustom(incomeSourceIndiv = testIncomeSourceBusiness).getSummary() shouldBe
          IndividualSummary(
            incomeSourceIndiv = testIncomeSourceBusiness,
            businessName = testBusinessName,
            selectedTaxYear = testSelectedTaxYearNext,
            accountingMethod = testAccountingMethod
          )
      }
      "income source is both property and business" in {
        testCacheMapCustom(incomeSourceIndiv = testIncomeSourceBoth).getSummary() shouldBe
          IndividualSummary(
            incomeSourceIndiv = testIncomeSourceBoth,
            businessName = testBusinessName,
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
          incomeSource = Some(testAgentIncomeSourceProperty)
        ).getAgentSummary() shouldBe AgentSummary(
          incomeSource = Some(testAgentIncomeSourceProperty),
          matchTaxYear = None,
          accountingPeriodDate = None,
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
          incomeSource = Some(testAgentIncomeSourceBoth)
        ).getAgentSummary() shouldBe
          AgentSummary(
            incomeSource = testAgentIncomeSourceBoth,
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
