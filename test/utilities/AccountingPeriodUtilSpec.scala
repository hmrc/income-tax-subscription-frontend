/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.{Matchers, OptionValues, WordSpecLike}


class AccountingPeriodUtilSpec extends WordSpecLike with Matchers with OptionValues {

  "AccountingPeriodUtil.getTaxEndYear" should {
    "return 2018 if the tax year ends between 6th April 2017 and 5th April 2018" in {
      val testPeriodLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2017"))
      val testPeriodBelowLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2017"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodLowBound) shouldBe 2018
      AccountingPeriodUtil.getTaxEndYear(testPeriodBelowLowBound) shouldBe 2017

      val testPeriodUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2018"))
      val testPeriodAboveUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2018"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodUpperBound) shouldBe 2018
      AccountingPeriodUtil.getTaxEndYear(testPeriodAboveUpperBound) shouldBe 2019
    }

    "return 2019 if the tax year ends between 6th April 2018 and 5th April 2019" in {
      val testPeriodLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2018"))
      val testPeriodBelowLowBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2018"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodLowBound) shouldBe 2019
      AccountingPeriodUtil.getTaxEndYear(testPeriodBelowLowBound) shouldBe 2018

      val testPeriodUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("5", "4", "2019"))
      val testPeriodAboveUpperBound = AccountingPeriodModel(TestModels.testStartDate, DateModel("6", "4", "2019"))
      AccountingPeriodUtil.getTaxEndYear(testPeriodUpperBound) shouldBe 2019
      AccountingPeriodUtil.getTaxEndYear(testPeriodAboveUpperBound) shouldBe 2020
    }
  }

  "AccountingPeriodUtil.getCurrentTaxYearStartDate" should {
    "return the start date for the current tax year" in {
      AccountingPeriodUtil.getCurrentTaxYearStartDate shouldBe DateModel("6", "4", (AccountingPeriodUtil.getCurrentTaxEndYear -1).toString)
    }
  }

  "AccountingPeriodUtil.getCurrentTaxYearEndDate" should {
    "return the end date for the current tax year" in {
      AccountingPeriodUtil.getCurrentTaxYearEndDate shouldBe DateModel("5", "4", AccountingPeriodUtil.getCurrentTaxEndYear.toString)
    }
  }

}
