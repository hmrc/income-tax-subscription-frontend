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

package services

import models._
import org.scalatest.Matchers._
import utils.UnitTestTrait

class CacheUtilSpec extends UnitTestTrait {

  import CacheUtil._
  import utils.TestModels._

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getIncomeSource() shouldBe None
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getAccountingPeriod() shouldBe None
      emptyCacheMap.getContactEmail() shouldBe None
      emptyCacheMap.getIncomeType() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getIncomeSource() shouldBe Some(testIncomeSourceBoth)
      testCacheMap.getBusinessName() shouldBe Some(testBusinessName)
      testCacheMap.getAccountingPeriod() shouldBe Some(testAccountingPeriod)
      testCacheMap.getContactEmail() shouldBe Some(testContactEmail)
      testCacheMap.getIncomeType() shouldBe Some(testIncomeType)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "The getSummary should populate the Summary model correctly" in {
      testCacheMap.getSummary() shouldBe
        SummaryModel(
          testIncomeSourceBoth,
          testAccountingPeriod,
          testBusinessName,
          testIncomeType,
          testContactEmail,
          testTerms
        )

      // for the property only journey, this should only populate the subset of views
      // relevant to the journey
      val overPopulatedPropertyCacheMap =
        testCacheMap(
          testIncomeSourceProperty,
          testAccountingPeriod,
          testBusinessName,
          testIncomeType,
          testContactEmail,
          testTerms
        )
      overPopulatedPropertyCacheMap.getSummary() shouldBe
        SummaryModel(
          testIncomeSourceProperty,
          contactEmail = testContactEmail,
          terms = testTerms
        )

      emptyCacheMap.getSummary() shouldBe SummaryModel()
    }

  }
}

