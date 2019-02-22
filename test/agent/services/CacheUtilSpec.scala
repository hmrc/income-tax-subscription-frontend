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

import _root_.core.utils.{TestConstants, UnitTestTrait}
import incometax.subscription.models.{AgentSummary, SummaryModel}
import org.scalatest.Matchers._

class CacheUtilSpec extends UnitTestTrait {

  import CacheUtil._
  import core.utils.TestModels._

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getIncomeSource() shouldBe None
      emptyCacheMap.getOtherIncome() shouldBe None
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getAccountingPeriodDate() shouldBe None
      emptyCacheMap.getAccountingMethod() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getIncomeSource() shouldBe Some(testIncomeSourceBoth)
      testCacheMap.getOtherIncome() shouldBe Some(testOtherIncomeNo)
      testCacheMap.getBusinessName() shouldBe Some(testBusinessName)
      testCacheMap.getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingMethod() shouldBe Some(testAccountingMethod)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "The getSummary should populate the Summary model correctly" in {
      testCacheMap.getSummary() shouldBe
        AgentSummary(
          testIncomeSourceBoth,
          testOtherIncomeNo,
          None, // match tax year
          testAccountingPeriodPriorCurrent,
          testAccountingPeriod,
          testBusinessName,
          None,
          None,
          None,
          testAccountingMethod,
          testTerms
        )

      // for the property only journey, this should only populate the subset of views
      // relevant to the journey
      val overPopulatedPropertyCacheMap =
      agent.utils.TestModels.testCacheMap(
        Some(agent.utils.TestModels.testIncomeSourceProperty),
        Some(agent.utils.TestModels.testOtherIncomeNo),
        Some(agent.utils.TestModels.testAccountingPeriodPriorCurrent),
        Some(agent.utils.TestModels.testAccountingPeriod),
        Some(agent.utils.TestModels.testBusinessName),
        Some(agent.utils.TestModels.testAccountingMethod),
        Some(agent.utils.TestModels.testTerms)
      )
      overPopulatedPropertyCacheMap.getSummary() shouldBe
        AgentSummary(
          testIncomeSourceProperty,
          otherIncome = testOtherIncomeNo,
          terms = testTerms
        )

      emptyCacheMap.getSummary() shouldBe AgentSummary()
    }

  }
}

