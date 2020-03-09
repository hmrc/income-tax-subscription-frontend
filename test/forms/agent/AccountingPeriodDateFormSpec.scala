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

package forms.agent

import java.time.LocalDate

import forms.agent.AccountingPeriodDateForm._
import forms.submapping.DateMapping._
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.DateModel
import models.individual.business.AccountingPeriodModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.data.FormError

class AccountingPeriodDateFormSpec extends PlaySpec with OneAppPerTest {

  val today: DateModel = DateModel.dateConvert(LocalDate.now())

  "The AccountingPeriodDateForm" should {
    "transform a valid request to the date form case class" in {
      val today = DateModel.dateConvert(LocalDate.now())
      val tomorrow = today.plusDays(1)

      val testInput = Map(
        startDate * dateDay -> today.day, startDate * dateMonth -> today.month, startDate * dateYear -> today.year,
        endDate * dateDay -> tomorrow.day, endDate * dateMonth -> tomorrow.month, endDate * dateYear -> tomorrow.year
      )

      val expected = AccountingPeriodModel(
        DateModel(today.day, today.month, today.year),
        DateModel(tomorrow.day, tomorrow.month, tomorrow.year))

      val actual = accountingPeriodDateForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "output the appropriate error messages for the start date" when {

      val empty = "agent.error.start_date.empty"
      val invalidChar = "agent.error.start_date.invalid_chars"
      val invalid = "agent.error.start_date.invalid"

      "the date is not supplied to the map" in {
        accountingPeriodDateForm.bind(DataMap.EmptyMap).errors must contain(FormError(startDate, empty))
      }

      "the date supplied is empty" in {
        accountingPeriodDateForm.bind(DataMap.emptyDate(startDate)).errors must contain(FormError(startDate, empty))
      }

      "it is a date with invalid characters" in {
        val invalidCharTest = accountingPeriodDateForm.bind(DataMap.date(startDate)("!X", "5", "X0X;"))
        invalidCharTest.errors must contain(FormError(startDate, invalidChar))
      }

      "it is an invalid date" in {
        val invalidTest = accountingPeriodDateForm.bind(DataMap.date(startDate)("29", "2", "2017"))
        invalidTest.errors must contain(FormError(startDate, invalid))
      }

    }

    "output an appropriate error for the end date" should {
      val empty = "agent.error.end_date.empty"
      val invalidChar = "agent.error.end_date.invalid_chars"
      val invalid = "agent.error.end_date.invalid"
      val violation = "agent.error.end_date_violation"

      "a date is not supplied to the map" in {
        val emptyDateInput0 = DataMap.EmptyMap
        val emptyTest0 = accountingPeriodDateForm.bind(emptyDateInput0)
        emptyTest0.errors must contain(FormError(endDate, empty))
      }

      "the date supplied to the map is empty" in {
        val emptyDateInput = DataMap.emptyDate(endDate)
        val emptyTest = accountingPeriodDateForm.bind(emptyDateInput)
        emptyTest.errors must contain(FormError(endDate, empty))
      }

      "it is a date with invalid characters" in {
        val invalidCharTest = accountingPeriodDateForm.bind(DataMap.date(endDate)("!X", "5", "X0X;"))
        invalidCharTest.errors must contain(FormError(endDate, invalidChar))
      }

      "it is an invalid date" in {
        val invalidDateInput = DataMap.date(endDate)("29", "2", "2017")
        val invalidTest = accountingPeriodDateForm.bind(invalidDateInput)
        invalidTest.errors must contain(FormError(endDate, invalid))
      }
    }


    "accept a valid date" in {

      val testStartDate = DateModel.dateConvert(LocalDate.now())
      val testEndDate = DateModel.dateConvert(LocalDate.now().plusDays(1))

      val testData = DataMap.date(startDate)(testStartDate.day, testStartDate.month, testStartDate.year) ++
        DataMap.date(endDate)(testEndDate.day, testEndDate.month, testEndDate.year)
      accountingPeriodDateForm isValidFor testData
    }
  }

}
