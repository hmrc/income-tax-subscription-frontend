/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import utilities.MockCurrentDateProvider

import java.time.LocalDate

class AccountingPeriodTestAccountingPeriodServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with BeforeAndAfterEach with MockCurrentDateProvider {


  class Setup(date: LocalDate = LocalDate.of(2019, 9, 1)) {
    val currentDate: LocalDate = date

    case object TestAccountingPeriodService extends AccountingPeriodService(mockCurrentDateProvider)

  }

  class SetupUpdateDates(date: LocalDate) {
    val currentDate: LocalDate = date
    val currentTaxYear: Int = TestAccountingPeriodService.currentTaxYear


    case object TestAccountingPeriodService extends AccountingPeriodService(mockCurrentDateProvider)

  }

  "Accounting Period eligibility" should {
    "return false" when {
      "the accounting period end date is less than the current tax year" in new Setup {
        val testStart: LocalDate = LocalDate.of(2018, 4, 6)
        val testEnd: LocalDate = LocalDate.of(2019, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe false
      }

      "the accounting period end date is a year more then the current tax year" in new Setup {
        val testStart: LocalDate = LocalDate.of(2020, 4, 7)
        val testEnd: LocalDate = LocalDate.of(2021, 4, 6)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe false
      }

      "the accounting period is greater than a year" in new Setup {
        val testStart: LocalDate = LocalDate.of(2019, 4, 6)
        val testEnd: LocalDate = LocalDate.of(2020, 4, 6)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe false
      }

      "the accounting period is less than a year" in new Setup(LocalDate.of(2020, 9, 1)) {
        val testStart: LocalDate = LocalDate.of(2020, 4, 6)
        val testEnd: LocalDate = LocalDate.of(2021, 4, 4)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe false
      }

      "the accounting period start date is less than the current tax year" in new Setup {
        val testStart: LocalDate = LocalDate.of(2019, 3, 31)
        val testEnd: LocalDate = LocalDate.of(2020, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe false
      }

      "the accounting period is less than the whole year" in new Setup {
        val testStart: LocalDate = LocalDate.of(2019, 4, 7)
        val testEnd: LocalDate = LocalDate.of(2020, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe false
      }

      "the accounting period end date is in the next tax year and is a period of exactly one year when user has a property income source" in new Setup {
        val testStart: LocalDate = LocalDate.of(2020, 4, 6)
        val testEnd: LocalDate = LocalDate.of(2021, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = true) shouldBe false
      }
    }

    "return true" when {
      "the accounting period end date is in the current tax year and is a period of exactly one year" in new Setup {
        val testStart: LocalDate = LocalDate.of(2019, 4, 6)
        val testEnd: LocalDate = LocalDate.of(2020, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe true
      }

      "the accounting period end date is in the current tax year and is a period of exactly one year when user has a property income source" in new Setup {
        val testStart: LocalDate = LocalDate.of(2019, 4, 6)
        val testEnd: LocalDate = LocalDate.of(2020, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = true) shouldBe true
      }

      "the accounting period end date is in the current tax year and is a period of exactly one year and signing up in the same year as the end date" in
        new Setup(LocalDate.of(2020, 1, 1)) {
          val testStart: LocalDate = LocalDate.of(2019, 4, 6)
          val testEnd: LocalDate = LocalDate.of(2020, 4, 5)

          mockCurrentDate(currentDate)

          TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe true
        }

      "the accounting period end date is in the current tax year and is a period of exactly one year and five days" in new Setup {
        val testStart: LocalDate = LocalDate.of(2019, 4, 1)
        val testEnd: LocalDate = LocalDate.of(2020, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe true
      }

      "the accounting period end date is in the following tax year and is a period of exactly one year and five days" in new Setup {
        val testStart: LocalDate = LocalDate.of(2020, 4, 1)
        val testEnd: LocalDate = LocalDate.of(2021, 4, 5)

        mockCurrentDate(currentDate)

        TestAccountingPeriodService.checkEligibleAccountingPeriod(testStart, testEnd, hasPropertyType = false) shouldBe true
      }
    }
  }

}
