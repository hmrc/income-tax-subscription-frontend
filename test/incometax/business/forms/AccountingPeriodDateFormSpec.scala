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

package incometax.business.forms

import java.time.LocalDate

import assets.MessageLookup
import core.forms.submapping.DateMapping
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import core.models.DateModel
import incometax.business.models.AccountingPeriodModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class AccountingPeriodDateFormSpec extends PlaySpec with OneAppPerTest {

  import DateMapping._
  import incometax.business.forms.AccountingPeriodDateForm._

  val today = DateModel.dateConvert(LocalDate.now())

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

    "when testing the validation" should {

      "output the appropriate error messages for the start date" when {

        "a date is not supplied" in {
          val empty = ErrorMessageFactory.error("error.start_date.empty")
          empty fieldErrorIs MessageLookup.Error.StartDate.empty
          empty summaryErrorIs MessageLookup.Error.StartDate.empty

          accountingPeriodDateForm.bind(DataMap.EmptyMap) assert startDate hasExpectedErrors empty
          accountingPeriodDateForm.bind(DataMap.emptyDate(startDate)) assert startDate hasExpectedErrors empty
        }

        "it is a date with invalid characters" in {
          val invalidChar = ErrorMessageFactory.error("error.start_date.invalid_chars")
          invalidChar fieldErrorIs MessageLookup.Error.StartDate.invalid_chars
          invalidChar summaryErrorIs MessageLookup.Error.StartDate.invalid_chars

          val invalidCharTest = accountingPeriodDateForm.bind(DataMap.date(startDate)("!X", "5", "X0X;"))
          invalidCharTest assert startDate hasExpectedErrors invalidChar
        }

        "it is an invalid date" in {
          val invalid = ErrorMessageFactory.error("error.start_date.invalid")
          invalid fieldErrorIs MessageLookup.Error.StartDate.invalid
          invalid summaryErrorIs MessageLookup.Error.StartDate.invalid

          val invalidTest = accountingPeriodDateForm.bind(DataMap.date(startDate)("29", "2", "2017"))
          invalidTest assert startDate hasExpectedErrors invalid
        }

        "it is before or during there current tax period" in {
          val beforeMin = ErrorMessageFactory.error("error.business_accounting_period.minStartDate")
          beforeMin fieldErrorIs MessageLookup.Error.BusinessAccountingPeriod.minStartDate
          beforeMin summaryErrorIs MessageLookup.Error.BusinessAccountingPeriod.minStartDate
          val invalidTestDate = LocalDate.parse(s"${LocalDate.now.getYear - 1}-04-06")

          val beforeMinTest = accountingPeriodDateForm.bind(DataMap.date(startDate)
          (invalidTestDate.getDayOfMonth.toString, invalidTestDate.getMonthValue.toString, invalidTestDate.getYear.toString))
          beforeMinTest assert startDate hasExpectedErrors beforeMin

          val validTestDate = LocalDate.now
          val minTest = accountingPeriodDateForm.bind(DataMap.date(startDate)
          (validTestDate.getDayOfMonth.toString, validTestDate.getMonthValue.toString, validTestDate.getYear.toString))
          minTest assert startDate doesNotHaveSpecifiedErrors beforeMin
        }
      }

      "output an appropriate error for the end date" when {

        "a date is not supplied" in {
          val empty = ErrorMessageFactory.error("error.end_date.empty")
          empty fieldErrorIs MessageLookup.Error.EndDate.empty
          empty summaryErrorIs MessageLookup.Error.EndDate.empty

          val emptyDateInput0 = DataMap.EmptyMap
          val emptyTest0 = accountingPeriodDateForm.bind(emptyDateInput0)
          emptyTest0 assert endDate hasExpectedErrors empty

          val emptyDateInput = DataMap.emptyDate(endDate)
          val emptyTest = accountingPeriodDateForm.bind(emptyDateInput)
          emptyTest assert endDate hasExpectedErrors empty

        }

        "it is a date with invalid characters" in {
          val invalidChar = ErrorMessageFactory.error("error.end_date.invalid_chars")
          invalidChar fieldErrorIs MessageLookup.Error.EndDate.invalid_chars
          invalidChar summaryErrorIs MessageLookup.Error.EndDate.invalid_chars

          val invalidCharTest = accountingPeriodDateForm.bind(DataMap.date(endDate)("!X", "5", "X0X;"))
          invalidCharTest assert endDate hasExpectedErrors invalidChar
        }

        "it is an invalid date" in {
          val invalid = ErrorMessageFactory.error("error.end_date.invalid")
          invalid fieldErrorIs MessageLookup.Error.EndDate.invalid
          invalid summaryErrorIs MessageLookup.Error.EndDate.invalid

          val invalidDateInput = DataMap.date(endDate)("29", "2", "2017")
          val invalidTest = accountingPeriodDateForm.bind(invalidDateInput)
          invalidTest assert endDate hasExpectedErrors invalid
        }

        "it is not after the start date" in {
          val violation = ErrorMessageFactory.error("error.end_date_violation")
          violation fieldErrorIs MessageLookup.Error.EndDate.end_violation
          violation summaryErrorIs MessageLookup.Error.EndDate.end_violation

          val endDateViolationInput = DataMap.date(startDate)(today.day, today.month, today.year)++
            DataMap.date(endDate)(today.day, today.month, today.year)

          val violationTest = accountingPeriodDateForm.bind(endDateViolationInput)
          violationTest assert endDate hasExpectedErrors violation
        }

        "it is in the past" in {
          val violation = ErrorMessageFactory.error("error.end_date_past")
          violation fieldErrorIs MessageLookup.Error.EndDate.end_past
          violation summaryErrorIs MessageLookup.Error.EndDate.end_past

          val pastDate = LocalDate.now().minusDays(1)

          val endDateViolationInput = DataMap.date(startDate)(pastDate.getDayOfMonth.toString, pastDate.getMonthValue.toString, pastDate.getYear.toString) ++
            DataMap.date(endDate)(pastDate.getDayOfMonth.toString, pastDate.getMonthValue.toString,  pastDate.getYear.toString)
          val violationTest = accountingPeriodDateForm.bind(endDateViolationInput)
          violationTest assert endDate hasExpectedErrors violation
        }


        "it is more than 24 months after the start date" in {
          val violation = ErrorMessageFactory.error("error.business_accounting_period.maxEndDate")
          violation fieldErrorIs MessageLookup.Error.BusinessAccountingPeriod.maxEndDate
          violation summaryErrorIs MessageLookup.Error.BusinessAccountingPeriod.maxEndDate

          val validStartDate = LocalDate.now
          val invalidEndDate = validStartDate.plusYears(2).plusDays(1)
          val validEndDate = validStartDate.plusYears(1).plusDays(364)

          val endDateViolationInput =
            DataMap.date(startDate)(validStartDate.getDayOfMonth.toString, validStartDate.getMonthValue.toString, validStartDate.getYear.toString) ++
              DataMap.date(endDate)(invalidEndDate.getDayOfMonth.toString, invalidEndDate.getMonthValue.toString, invalidEndDate.getYear.toString)

          val violationTest = accountingPeriodDateForm.bind(endDateViolationInput)
          violationTest assert endDate hasExpectedErrors violation

          val endDateNoViolationInput =
            DataMap.date(startDate)(validStartDate.getDayOfMonth.toString, validStartDate.getMonthValue.toString, validStartDate.getYear.toString) ++
              DataMap.date(endDate)(validEndDate.getDayOfMonth.toString, validEndDate.getMonthValue.toString, validEndDate.getYear.toString)

          val noViolationTest = accountingPeriodDateForm.bind(endDateNoViolationInput)
          noViolationTest assert endDate doesNotHaveSpecifiedErrors violation
        }
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


  "The user tries to submit a accounting period in the past" in {
    val testStartDate = DateModel.dateConvert(LocalDate.now())
    val testEndDate = DateModel.dateConvert(LocalDate.now().plusDays(1))

    val testData = DataMap.date(startDate)(testStartDate.day, testStartDate.month, testStartDate.year) ++
      DataMap.date(endDate)(testEndDate.day, testEndDate.month, testEndDate.year)
  }

}
