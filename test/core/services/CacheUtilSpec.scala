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
      emptyCacheMap.getAreYouSelfEmployed() shouldBe None
      emptyCacheMap.getIncomeSourceType() shouldBe None
      emptyCacheMap.getOtherIncome() shouldBe None
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getBusinessPhoneNumber() shouldBe None
      emptyCacheMap.getBusinessAddress() shouldBe None
      emptyCacheMap.getBusinessStartDate() shouldBe None
      emptyCacheMap.getMatchTaxYear() shouldBe None
      emptyCacheMap.getEnteredAccountingPeriodDate() shouldBe None
      emptyCacheMap.getAccountingPeriodDate() shouldBe None
      emptyCacheMap.getSelectedTaxYear() shouldBe None
      emptyCacheMap.getAccountingMethod() shouldBe None
      emptyCacheMap.getPropertyAccountingMethod() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getRentUkProperty() shouldBe Some(testRentUkProperty_property_and_other)
      testCacheMap.getAreYouSelfEmployed() shouldBe Some(testAreYouSelfEmployed_yes)
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
      testCacheMap.getPropertyAccountingMethod().contains(testAccountingMethodProperty) shouldBe true
      testCacheMap.getPropertyAccountingMethod() shouldBe Some(testAccountingMethodProperty)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "getAccountingPeriodDate" when {
      "the income source is property" should {
        "return none even if accounting period is filled in" in {
          testCacheMapCustom(
            rentUkProperty = testRentUkProperty_property_only,
            areYouSelfEmployed = None,
            matchTaxYear = testMatchTaxYearYes).getAccountingPeriodDate() shouldBe None
        }
      }

      "the income source is business or both" when {
        "match tax year is yes" should {
          "return the accounting period of the current tax year" in {
            testCacheMapCustom(incomeSource = testIncomeSourceBusiness, matchTaxYear = testMatchTaxYearYes).getAccountingPeriodDate() shouldBe Some(getCurrentTaxYear)
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              areYouSelfEmployed = testAreYouSelfEmployed_yes,
              matchTaxYear = testMatchTaxYearYes).getAccountingPeriodDate() shouldBe Some(getCurrentTaxYear)
          }
        }

        "match tax year is no" should {
          "return the entered accounting period" in {
            testCacheMapCustom(incomeSource = testIncomeSourceBusiness, matchTaxYear = testMatchTaxYearNo).getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              areYouSelfEmployed = testAreYouSelfEmployed_yes,
              matchTaxYear = testMatchTaxYearNo).getAccountingPeriodDate() shouldBe Some(testAccountingPeriod)
          }
        }
      }
    }

    "The getSummary should populate the Summary model correctly" when {
      "income source is just property" in {
        testCacheMapCustom(rentUkProperty = testRentUkProperty_property_only, areYouSelfEmployed = None).getSummary() shouldBe
          IndividualSummary(
            rentUkProperty = testRentUkProperty_property_only,
            areYouSelfEmployed = None,
            otherIncome = testOtherIncomeNo,
            accountingMethodProperty = testAccountingMethodProperty,
            terms = testTerms
          )
      }
      "income source is just business" in {
        testCacheMapCustom(rentUkProperty = testRentUkProperty_no_property, areYouSelfEmployed = testAreYouSelfEmployed_yes).getSummary() shouldBe
          IndividualSummary(
            rentUkProperty = testRentUkProperty_no_property,
            areYouSelfEmployed = testAreYouSelfEmployed_yes,
            otherIncome = testOtherIncomeNo,
            matchTaxYear = testMatchTaxYearNo,
            accountingPeriod = testAccountingPeriod,
            businessName = testBusinessName,
            businessPhoneNumber = testBusinessPhoneNumber,
            businessAddress = testAddress,
            businessStartDate = testBusinessStartDate,
            selectedTaxYear = testSelectedTaxYearNext,
            accountingMethod = testAccountingMethod,
            terms = testTerms
          )
      }
      "income source is both property and business" in {
        testCacheMapCustom(rentUkProperty = testRentUkProperty_property_and_other, areYouSelfEmployed = testAreYouSelfEmployed_yes).getSummary() shouldBe
          IndividualSummary(
            rentUkProperty = testRentUkProperty_property_and_other,
            areYouSelfEmployed = testAreYouSelfEmployed_yes,
            otherIncome = testOtherIncomeNo,
            matchTaxYear = testMatchTaxYearNo,
            accountingPeriod = testAccountingPeriod,
            businessName = testBusinessName,
            businessPhoneNumber = testBusinessPhoneNumber,
            businessAddress = testAddress,
            businessStartDate = testBusinessStartDate,
            accountingMethod = testAccountingMethod,
            accountingMethodProperty = testAccountingMethodProperty,
            terms = testTerms
          )
      }
      "income source is neither property or business" in {
        emptyCacheMap.getSummary() shouldBe IndividualSummary()
      }
    }
  }

}
