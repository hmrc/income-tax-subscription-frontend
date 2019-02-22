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

package core.services

import core.config.featureswitch.FeatureSwitching
import core.utils.UnitTestTrait
import incometax.subscription.models.{Both, IndividualSummary}
import incometax.util.AccountingPeriodUtil.getCurrentTaxYear
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Matchers._

class CacheUtilSpec extends UnitTestTrait
  with FeatureSwitching
  with BeforeAndAfterEach {

  import core.services.CacheUtil._
  import core.utils.TestModels._

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getRentUkProperty() shouldBe None
      emptyCacheMap.getWorkForYourself() shouldBe None
      emptyCacheMap.getIncomeSourceType() shouldBe None
      emptyCacheMap.getOtherIncome() shouldBe None
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getBusinessPhoneNumber() shouldBe None
      emptyCacheMap.getBusinessAddress() shouldBe None
      emptyCacheMap.getBusinessStartDate() shouldBe None
      emptyCacheMap.getMatchTaxYear() shouldBe None
      emptyCacheMap.getEnteredAccountingPeriodDate() shouldBe None
      emptyCacheMap.getAccountingPeriodDate() shouldBe None
      emptyCacheMap.getAccountingMethod() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getRentUkProperty() shouldBe Some(testRentUkProperty_property_and_other)
      testCacheMap.getWorkForYourself() shouldBe Some(testWorkForYourself_yes)
      testCacheMap.getIncomeSourceType() shouldBe Some(Both)
      testCacheMap.getOtherIncome() shouldBe Some(testOtherIncomeNo)
      testCacheMap.getBusinessName() shouldBe Some(testBusinessName)
      testCacheMap.getBusinessPhoneNumber() shouldBe Some(testBusinessPhoneNumber)
      testCacheMap.getBusinessAddress() shouldBe Some(testAddress)
      testCacheMap.getBusinessStartDate() shouldBe Some(testBusinessStartDate)
      testCacheMap.getMatchTaxYear() shouldBe Some(testMatchTaxYearNo)
      testCacheMap.getEnteredAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
      testCacheMap.getAccountingMethod() shouldBe Some(testAccountingMethod)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "getAccountingPeriodDate" when {
      "the income source is property" should {
        "return none even if accounting period is filled in" in {
          testCacheMapCustom(
            rentUkProperty = testRentUkProperty_property_only,
            workForYourself = None,
            matchTaxYear = testMatchTaxYearYes).getAccountingPeriodDate() shouldBe None
        }
      }

      "the income source is business or both" when {
        "match tax year is yes" should {
          "return the accounting period of the current tax year" in {
            testCacheMapCustom(incomeSource = testIncomeSourceBusiness, matchTaxYear = testMatchTaxYearYes).getAccountingPeriodDate() shouldBe Some(getCurrentTaxYear)
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              workForYourself = testWorkForYourself_yes,
              matchTaxYear = testMatchTaxYearYes).getAccountingPeriodDate() shouldBe Some(getCurrentTaxYear)
          }
        }

        "match tax year is no" should {
          "return the entered accounting period" in {
            testCacheMapCustom(incomeSource = testIncomeSourceBusiness, matchTaxYear = testMatchTaxYearNo).getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              workForYourself = testWorkForYourself_yes,
              matchTaxYear = testMatchTaxYearNo).getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
          }
        }
      }
    }

    "The getSummary should populate the Summary model correctly" in {
      testCacheMap.getSummary() shouldBe
        IndividualSummary(
          rentUkProperty = testRentUkProperty_property_and_other,
          workForYourself = testWorkForYourself_yes,
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
        rentUkProperty = testRentUkProperty_property_only,
        workForYourself = None,
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
        IndividualSummary(
          rentUkProperty = testRentUkProperty_property_only,
          workForYourself = None,
          otherIncome = testOtherIncomeNo,
          terms = testTerms
        )

      emptyCacheMap.getSummary() shouldBe IndividualSummary()
    }
  }

}

