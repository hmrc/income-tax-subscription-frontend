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

import agent.assets.MessageLookup
import core.models.DateModel
import forms.submapping.DateMapping
import forms.validation.ErrorMessageFactory
import forms.validation.testutils._
import incometax.business.models.AccountingPeriodModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._
import forms.validation.testutils.DataMap.DataMap
import DateMapping._
import forms.agent.AccountingPeriodDateForm._

class AccountingPeriodDateFormSpec extends PlaySpec with OneAppPerTest {



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
          val empty = ErrorMessageFactory.error("agent.error.start_date.empty")
          empty fieldErrorIs MessageLookup.Error.StartDate.empty
          empty summaryErrorIs MessageLookup.Error.StartDate.empty

          accountingPeriodDateForm.bind(DataMap.EmptyMap) assert startDate hasExpectedErrors empty
          accountingPeriodDateForm.bind(DataMap.emptyDate(startDate)) assert startDate hasExpectedErrors empty
        }

        "it is a date with invalid characters" in {
          val invalidChar = ErrorMessageFactory.error("agent.error.start_date.invalid_chars")
          invalidChar fieldErrorIs MessageLookup.Error.StartDate.invalid_chars
          invalidChar summaryErrorIs MessageLookup.Error.StartDate.invalid_chars

          val invalidCharTest = accountingPeriodDateForm.bind(DataMap.date(startDate)("!X", "5", "X0X;"))
          invalidCharTest assert startDate hasExpectedErrors invalidChar
        }

        "it is an invalid date" in {
          val invalid = ErrorMessageFactory.error("agent.error.start_date.invalid")
          invalid fieldErrorIs MessageLookup.Error.StartDate.invalid
          invalid summaryErrorIs MessageLookup.Error.StartDate.invalid

          val invalidTest = accountingPeriodDateForm.bind(DataMap.date(startDate)("29", "2", "2017"))
          invalidTest assert startDate hasExpectedErrors invalid
        }

      }

      "output an appropriate error for the end date" when {

        "a date is not supplied" in {
          val empty = ErrorMessageFactory.error("agent.error.end_date.empty")
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
          val invalidChar = ErrorMessageFactory.error("agent.error.end_date.invalid_chars")
          invalidChar fieldErrorIs MessageLookup.Error.EndDate.invalid_chars
          invalidChar summaryErrorIs MessageLookup.Error.EndDate.invalid_chars

          val invalidCharTest = accountingPeriodDateForm.bind(DataMap.date(endDate)("!X", "5", "X0X;"))
          invalidCharTest assert endDate hasExpectedErrors invalidChar
        }

        "it is an invalid date" in {
          val invalid = ErrorMessageFactory.error("agent.error.end_date.invalid")
          invalid fieldErrorIs MessageLookup.Error.EndDate.invalid
          invalid summaryErrorIs MessageLookup.Error.EndDate.invalid

          val invalidDateInput = DataMap.date(endDate)("29", "2", "2017")
          val invalidTest = accountingPeriodDateForm.bind(invalidDateInput)
          invalidTest assert endDate hasExpectedErrors invalid
        }

        "it is not after the start date" in {
          val violation = ErrorMessageFactory.error("agent.error.end_date_violation")
          violation fieldErrorIs MessageLookup.Error.EndDate.end_violation
          violation summaryErrorIs MessageLookup.Error.EndDate.end_violation
          

          val endDateViolationInput = DataMap.date(startDate)(today.day, today.month, today.year)++
            DataMap.date(endDate)(today.day, today.month, today.year)

          val violationTest = accountingPeriodDateForm.bind(endDateViolationInput)
          violationTest assert endDate hasExpectedErrors violation
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

}