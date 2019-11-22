/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.services

import _root_.core.utils.UnitTestTrait
import incometax.subscription.models.AgentSummary
import org.scalatest.Matchers._

class CacheUtilSpec extends UnitTestTrait {

  import CacheUtil._
  import agent.utils.TestModels._

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getIncomeSource() shouldBe None
      emptyCacheMap.getOtherIncome() shouldBe None
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getAccountingPeriodDate() shouldBe None
      emptyCacheMap.getAccountingMethod() shouldBe None
      emptyCacheMap.getAccountingMethodProperty() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getIncomeSource() shouldBe Some(testIncomeSourceBoth)
      testCacheMap.getOtherIncome() shouldBe Some(testOtherIncomeNo)
      testCacheMap.getBusinessName() shouldBe Some(testBusinessName)
      testCacheMap.getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingPeriodPrior() shouldBe Some(testAccountingPeriodPriorCurrent)
      testCacheMap.getAccountingMethod() shouldBe Some(testAccountingMethod)
      testCacheMap.getAccountingMethodProperty() shouldBe Some(testAccountingMethodProperty)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "The getSummary should populate the Summary model correctly" when {
      "the income type is property" in {
        testCacheMapCustom(incomeSource = Some(testIncomeSourceProperty), accountingPeriodPrior = None,
          accountingPeriodDate = None, businessName = None, accountingMethod = None)
          .getSummary() shouldBe AgentSummary(
            incomeSource = Some(testIncomeSourceProperty),
            otherIncome = Some(testOtherIncomeNo),
            accountingPeriodPrior = None,
            accountingPeriodDate = None,
            businessName = None,
            accountingMethod = None,
            accountingMethodProperty = Some(testAccountingMethodProperty),
            terms = Some(testTerms)
          )
      }

      "the income type is business" in {
        val agentSummaryBusinessIncome = AgentSummary(
          incomeSource = Some(testIncomeSourceBusiness),
          otherIncome = Some(testOtherIncomeNo),
          accountingPeriodPrior = Some(testAccountingPeriodPriorCurrent),
          accountingPeriodDate = Some(testAccountingPeriod),
          businessName = Some(testBusinessName),
          accountingMethod = Some(testAccountingMethod),
          accountingMethodProperty = None,
          terms = Some(testTerms)
        )

        testCacheMapCustom(incomeSource = Some(testIncomeSourceBusiness), accountingMethodProperty = None)
          .getSummary() shouldBe agentSummaryBusinessIncome
      }

      "the income type is both" in {
        testCacheMapCustom().getSummary() shouldBe
          AgentSummary(
            incomeSource = testIncomeSourceBoth,
            otherIncome = testOtherIncomeNo,
            accountingPeriodPrior = testAccountingPeriodPriorCurrent,
            accountingPeriodDate = testAccountingPeriod,
            businessName = testBusinessName,
            accountingMethod = testAccountingMethod,
            accountingMethodProperty = testAccountingMethodProperty,
            terms = testTerms
          )

        emptyCacheMap.getSummary() shouldBe AgentSummary()
      }
    }

  }
}

