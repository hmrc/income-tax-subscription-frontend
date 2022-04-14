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

package forms.formatters

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import forms.formatters.DateErrorMapping._
import forms.formatters.DateModelMapping.HtmlIds
import forms.formatters.DateValidation.{Day, DayField, DayMonth, Month, MonthField, ValidDate, Year, YearField}
import models.DateModel
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DateValidationSpec extends AnyWordSpec with Matchers {
  private val testKey = "test-key"
  private val testKeys = HtmlIds(testKey)

  "DateField" should {
    "reorder in alphabetical order" in {
      List(YearField, MonthField, DayField, YearField).distinct.sorted.mkString("_") mustBe "day_month_year"
    }
  }

  "validateDay" should {
    "return an invalid result" when {
      "given an empty map" in {
        DateValidation.validateDay(Map(), testKeys) mustBe Invalid(NonEmptyList.of(EmptyDay))
      }
      "given an empty string" in {
        DateValidation.validateDay(Map(s"$testKey-dateDay" -> ""), testKeys) mustBe Invalid(NonEmptyList.of(EmptyDay))
      }
      "given an invalid string" in {
        DateValidation.validateDay(Map(s"$testKey-dateDay" -> "invalid"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }
      "given an negative number" in {
        DateValidation.validateDay(Map(s"$testKey-dateDay" -> "-1"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }
      "given an invalid day" in {
        DateValidation.validateDay(Map(s"$testKey-dateDay" -> "42"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }
    }

    "return an valid result" when {
      "given a valid day" in {
        DateValidation.validateDay(Map(s"$testKey-dateDay" -> "1"), testKeys) mustBe Valid(Day(1))
      }
    }
  }

  "validateMonth" should {
    "return an invalid result" when {
      "given an empty map" in {
        DateValidation.validateMonth(Map(), testKeys) mustBe Invalid(NonEmptyList.of(EmptyMonth))
      }
      "given an empty string" in {
        DateValidation.validateMonth(Map(s"$testKey-dateMonth" -> ""), testKeys) mustBe Invalid(NonEmptyList.of(EmptyMonth))
      }
      "given an invalid string" in {
        DateValidation.validateMonth(Map(s"$testKey-dateMonth" -> "invalid"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidMonth))
      }
      "given an negative number" in {
        DateValidation.validateMonth(Map(s"$testKey-dateMonth" -> "-1"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidMonth))
      }
      "given an invalid month" in {
        DateValidation.validateMonth(Map(s"$testKey-dateMonth" -> "42"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidMonth))
      }
    }

    "return an valid result" when {
      "given a valid month" in {
        DateValidation.validateMonth(Map(s"$testKey-dateMonth" -> "1"), testKeys) mustBe Valid(Month(1))
      }
    }
  }

  "validateYear" should {
    "return an invalid result" when {
      "given an empty map" in {
        DateValidation.validateYear(Map(), testKeys) mustBe Invalid(NonEmptyList.of(EmptyYear))
      }
      "given an empty string" in {
        DateValidation.validateYear(Map(s"$testKey-dateYear" -> ""), testKeys) mustBe Invalid(NonEmptyList.of(EmptyYear))
      }
      "given an invalid string" in {
        DateValidation.validateYear(Map(s"$testKey-dateYear" -> "invalid"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidYear))
      }
      "given an negative number" in {
        DateValidation.validateYear(Map(s"$testKey-dateYear" -> "-1"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidYear))
      }
      "given an invalid year" in {
        DateValidation.validateYear(Map(s"$testKey-dateYear" -> "999"), testKeys) mustBe Invalid(NonEmptyList.of(InvalidYearLength))
      }
    }

    "return an valid result" when {
      "given a valid year" in {
        DateValidation.validateYear(Map(s"$testKey-dateYear" -> "2022"), testKeys) mustBe Valid(Year(2022))
      }
    }
  }

  "validateDayMonth" should {
    "return an invalid result" when {
      "given an invalid day of month for February" in {
        DateValidation.validateDayMonth(DayMonth(Day(30), Month(2))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }

      "given an invalid day of month for April" in {
        DateValidation.validateDayMonth(DayMonth(Day(31), Month(4))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }

      "given an invalid day of month for June" in {
        DateValidation.validateDayMonth(DayMonth(Day(31), Month(6))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }

      "given an invalid day of month for September" in {
        DateValidation.validateDayMonth(DayMonth(Day(31), Month(9))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }

      "given an invalid day of month for November" in {
        DateValidation.validateDayMonth(DayMonth(Day(31), Month(11))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }

      "given an invalid day of month for December" in {
        DateValidation.validateDayMonth(DayMonth(Day(32), Month(12))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }
    }

    "return an valid result" when {
      "given a valid day of month for February" in {
        DateValidation.validateDayMonth(DayMonth(Day(29), Month(2))) mustBe Valid(DayMonth(Day(29), Month(2)))
      }

      "given a valid day of month for April" in {
        DateValidation.validateDayMonth(DayMonth(Day(30), Month(4))) mustBe Valid(DayMonth(Day(30), Month(4)))
      }
    }
  }

  "validateDate" should {
    "return an invalid result" when {
      "given an invalid day of month for February" in {
        DateValidation.validateDate(ValidDate(Day(29), Month(2), Year(2022))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }

      "given an invalid day of month for November" in {
        DateValidation.validateDate(ValidDate(Day(31), Month(11), Year(2022))) mustBe Invalid(NonEmptyList.of(InvalidDay))
      }

      "given an invalid date" in {
        DateValidation.validateDate(ValidDate(Day(-29), Month(2), Year(2022))) mustBe Invalid(NonEmptyList.of(InvalidDate))
      }

      "given a date too early" in {
        DateValidation.validateDate(
          ValidDate(Day(29), Month(2), Year(2020)), minDate = Some(DateModel("1", "3", "2020").toLocalDate)
        ) mustBe Invalid(NonEmptyList.of(TooEarly))
      }

      "given a date too late" in {
        DateValidation.validateDate(
          ValidDate(Day(22), Month(2), Year(2022)), maxDate = Some(DateModel("1", "3", "2020").toLocalDate)
        ) mustBe Invalid(NonEmptyList.of(TooLate))
      }
    }

    "return an valid result" when {
      "given a valid day of month for February" in {
        DateValidation.validateDate(ValidDate(Day(29), Month(2), Year(2020))) mustBe Valid(DateModel("29", "2", "2020"))
      }
    }
  }

  "DateValidation max day for month" when {
    "month is february, in a leap year" should {
      "give 29" in {
        DateValidation.maxDayForMonth(2, 2000) must be(29)
      }
    }
    "month is february, not in a leap year" should {
      "give 29" in {
        DateValidation.maxDayForMonth(2, 2001) must be(28)
      }
    }
    "month is thermidor" should {
      "give last day of longest month" in {
        DateValidation.maxDayForMonth(13, 2001) must be(31)
      }
    }
    "crazy corner case of 1900 not being a leap year" should {
      "give 28" in {
        DateValidation.maxDayForMonth(2, 1900) must be(28)
      }
    }
  }
}
