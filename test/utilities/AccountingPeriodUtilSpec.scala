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

package utilities

import models.DateModel
import models.common.AccountingPeriodModel
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.LocalDate


class AccountingPeriodUtilSpec extends AnyWordSpecLike with Matchers with OptionValues {

  "AccountingPeriodUtil.getTaxEndYear" should {
    "return 2018 if the tax year ends between 6th April 2017 and 5th April 2018" in {
      val testPeriodLowBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("6", "4", "2017"))
      val testPeriodBelowLowBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("5", "4", "2017"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodLowBound) shouldBe 2018
      AccountingPeriodUtil.getTaxEndYear(testPeriodBelowLowBound) shouldBe 2017

      val testPeriodUpperBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("5", "4", "2018"))
      val testPeriodAboveUpperBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("6", "4", "2018"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodUpperBound) shouldBe 2018
      AccountingPeriodUtil.getTaxEndYear(testPeriodAboveUpperBound) shouldBe 2019
    }

    "return 2019 if the tax year ends between 6th April 2018 and 5th April 2019" in {
      val testPeriodLowBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("6", "4", "2018"))
      val testPeriodBelowLowBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("5", "4", "2018"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodLowBound) shouldBe 2019
      AccountingPeriodUtil.getTaxEndYear(testPeriodBelowLowBound) shouldBe 2018

      val testPeriodUpperBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("5", "4", "2019"))
      val testPeriodAboveUpperBound = AccountingPeriodModel(TestModels.testStartDateThisYear, DateModel("6", "4", "2019"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodUpperBound) shouldBe 2019
      AccountingPeriodUtil.getTaxEndYear(testPeriodAboveUpperBound) shouldBe 2020
    }
  }

  "AccountingPeriodUtil.getCurrentTaxYearStartDate" should {
    "return the start date for the current tax year" in {
      AccountingPeriodUtil.getCurrentTaxYear.startDate shouldBe DateModel("6", "4", (AccountingPeriodUtil.getCurrentTaxEndYear -1).toString)
    }
  }

  "AccountingPeriodUtil.getCurrentTaxYearEndDate" should {
    "return the end date for the current tax year" in {
      AccountingPeriodUtil.getCurrentTaxYear.endDate shouldBe DateModel("5", "4", AccountingPeriodUtil.getCurrentTaxEndYear.toString)
    }
  }

  "AccountingPeriodUtil.getEndOfPeriodStatementDate" when {
    "for current tax year" should {
      "return the end of period statement date for the current tax year" in {
        val now: LocalDate = LocalDate.now
        val endOfPeriodStatementYear: Int = AccountingPeriodUtil.getTaxEndYear(now) + 1
        AccountingPeriodUtil.getEndOfPeriodStatementDate(false) shouldBe LocalDate.of(endOfPeriodStatementYear, 1, 31)
      }
    }
    "for next tax year" should {
      "return the end of period statement date for the next tax year" in {
        val now: LocalDate = LocalDate.now
        val endOfPeriodStatementYear: Int = AccountingPeriodUtil.getTaxEndYear(now) + 2
        AccountingPeriodUtil.getEndOfPeriodStatementDate(true) shouldBe LocalDate.of(endOfPeriodStatementYear, 1, 31)
      }
    }
  }

  "AccountingPeriodUtil.getFinalDeclarationDate" when {
    "for current tax year" should {
      "return the final declaration date for the current tax year" in {
        val now: LocalDate = LocalDate.now
        val finalDeclarationYear: Int = AccountingPeriodUtil.getTaxEndYear(now) + 1
        AccountingPeriodUtil.getFinalDeclarationDate(false) shouldBe LocalDate.of(finalDeclarationYear, 1, 31)
      }
    }
    "for next tax year" should {
      "return the final declaration date for the next tax year" in {
        val now: LocalDate = LocalDate.now
        val finalDeclarationYear: Int = AccountingPeriodUtil.getTaxEndYear(now) + 2
        AccountingPeriodUtil.getFinalDeclarationDate(true) shouldBe LocalDate.of(finalDeclarationYear, 1, 31)
      }
    }
  }

}
