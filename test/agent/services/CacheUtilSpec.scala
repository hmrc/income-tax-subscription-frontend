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

package agent.services

import _root_.core.utils.{TestConstants, UnitTestTrait}
import incometax.subscription.models.SummaryModel
import org.scalatest.Matchers._

class CacheUtilSpec extends UnitTestTrait {

  import CacheUtil._
  import core.utils.TestModels._

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getNino() shouldBe None
      emptyCacheMap.getIncomeSource() shouldBe None
      emptyCacheMap.getOtherIncome() shouldBe None
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getAccountingPeriodPrior() shouldBe None
      emptyCacheMap.getAccountingPeriodDate() shouldBe None
      emptyCacheMap.getAccountingMethod() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getNino() shouldBe Some(TestConstants.testNino)
      testCacheMap.getIncomeSource() shouldBe Some(testIncomeSourceBoth)
      testCacheMap.getOtherIncome() shouldBe Some(testOtherIncomeNo)
      testCacheMap.getBusinessName() shouldBe Some(testBusinessName)
      testCacheMap.getAccountingPeriodPrior() shouldBe Some(testAccountingPeriodPriorCurrent)
      testCacheMap.getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingMethod() shouldBe Some(testAccountingMethod)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "The getSummary should populate the Summary model correctly" in {
      testCacheMap.getSummary() shouldBe
        SummaryModel(
          testIncomeSourceBoth,
          testOtherIncomeNo,
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
          agent.utils.TestModels.testClientDetails,
          agent.utils.TestModels.testClientDetails.ninoInBackendFormat,
          agent.utils.TestModels.testIncomeSourceProperty,
          agent.utils.TestModels.testOtherIncomeNo,
          agent.utils.TestModels.testAccountingPeriodPriorCurrent,
          agent.utils.TestModels.testAccountingPeriod,
          agent.utils.TestModels.testBusinessName,
          agent.utils.TestModels.testAccountingMethod,
          agent.utils.TestModels.testTerms)
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

