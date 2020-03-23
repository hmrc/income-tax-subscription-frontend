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

package models.individual.business

import models.DateModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import utilities.TestModels

class AccountingPeriodModelSpec extends PlaySpec {

  "the AccountingPeriodModel.adjustedTaxYear" when {
    "the end date is before 6 April 2018" should {
      "return the next tax year" in {
        val testAccountingPeriod = TestModels.testAccountingPeriod.copy(endDate = DateModel("5", "4", "2018"))
        testAccountingPeriod.adjustedTaxYear shouldBe AccountingPeriodModel(DateModel("6", "4", "2018"), DateModel("5", "4", "2019"))
      }
    }

    "the end date is on or after 6 April 2018" should {
      "return itself" in {
        val testAccountingPeriod = TestModels.testAccountingPeriod.copy(endDate = DateModel("6", "4", "2018"))
        testAccountingPeriod.adjustedTaxYear shouldBe testAccountingPeriod
        val testAccountingPeriod2 = TestModels.testAccountingPeriod.copy(endDate = DateModel("7", "4", "2018"))
        testAccountingPeriod2.adjustedTaxYear shouldBe testAccountingPeriod2
      }
    }
  }

}
