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

package forms.formatters

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import play.api.data.format.Formatter

import java.time.LocalDate

class LocalDateFormatterSpec extends AnyWordSpecLike with Matchers {

  def localDateFormatter(): Formatter[LocalDate] = new LocalDateFormatter(
    invalidKey = "error.invalid",
    allRequiredKey = "error.allRequired",
    twoRequiredKey = "error.twoRequired",
    requiredKey = "error.required",
    invalidYearKey = "error.invalidYear"
  )

  val bindKey: String = "testKey"

  def input(day: Option[String], month: Option[String], year: Option[String]): Map[String, String] = List(
    s"$bindKey-dateDay" -> day,
    s"$bindKey-dateMonth" -> month,
    s"$bindKey-dateYear" -> year
  ).collect {
    case (key, Some(value)) => (key, value)
  }.toMap

  "bind" should {
    "return a valid localDate when all fields are valid" when {
      "a valid date is provided" in {
        val result = localDateFormatter().bind(bindKey, input(Some("31"), Some("1"), Some("2025")))
        result mustBe Right(LocalDate.of(2025, 1, 31))
      }
      "a leap year is provided" in {
        val result = localDateFormatter().bind(bindKey, input(Some("29"), Some("2"), Some("2024")))
        result mustBe Right(LocalDate.of(2024, 2, 29))
      }
      "day and month has leading zeros" in {
        val result = localDateFormatter().bind(bindKey, input(Some("01"), Some("01"), Some("2025")))
        result mustBe Right(LocalDate.of(2025, 1, 1))
      }
      "month is provided as a the full month name" in {
        val result = localDateFormatter().bind(bindKey, input(Some("31"), Some("January"), Some("2025")))
        result mustBe Right(LocalDate.of(2025, 1, 31))
      }
      "month is provided as a the shortened month name" in {
        val result = localDateFormatter().bind(bindKey, input(Some("31"), Some("DEC"), Some("2025")))
        result mustBe Right(LocalDate.of(2025, 12, 31))
      }
    }
    "return a FormError with no args" when {
      "all fields are empty" in {
        val result = localDateFormatter().bind(bindKey, input(None, None, None))
        result mustBe Left(Seq(FormError(bindKey, "error.allRequired")))
      }
      "there are multiple invalid fields" which {
        "has an invalid day and month" in {
          val result = localDateFormatter().bind(bindKey, input(Some("32"), Some("13"), Some("2025")))
          result mustBe Left(Seq(FormError(bindKey, "error.invalid")))
        }
        "has an invalid day and year" in {
          val result = localDateFormatter().bind(bindKey, input(Some("32"), Some("1"), Some("2o25")))
          result mustBe Left(Seq(FormError(bindKey, "error.invalid")))
        }
        "has an invalid month and year" in {
          val result = localDateFormatter().bind(bindKey, input(Some("1"), Some("13"), Some("2o25")))
          result mustBe Left(Seq(FormError(bindKey, "error.invalid")))
        }
      }
    }
    "return a FormError with the field name as an arg" when {
      "one field is empty" when {
        "day is empty" in {
          val result = localDateFormatter().bind(bindKey, input(Some(""), Some("1"), Some("2025")))
          result mustBe Left(Seq(FormError(bindKey, "error.required", Seq("day"))))
        }
        "month is empty" in {
          val result = localDateFormatter().bind(bindKey, input(Some("1"), Some(""), Some("2025")))
          result mustBe Left(Seq(FormError(bindKey, "error.required", Seq("month"))))
        }
        "year is empty" in {
          val result = localDateFormatter().bind(bindKey, input(Some("31"), Some("12"), Some("")))
          result mustBe Left(Seq(FormError(bindKey, "error.required", Seq("year"))))
        }
      }
      "two fields are empty" when {
        "day and month are empty" in {
          val result = localDateFormatter().bind(bindKey, input(Some(""), Some(""), Some("2025")))
          result mustBe Left(Seq(FormError(bindKey, "error.twoRequired", Seq("day", "month"))))
        }
        "day and year are empty" in {
          val result = localDateFormatter().bind(bindKey, input(Some(""), Some("12"), Some("")))
          result mustBe Left(Seq(FormError(bindKey, "error.twoRequired", Seq("day", "year"))))
        }
        "month and year are empty" in {
          val result = localDateFormatter().bind(bindKey, input(Some("15"), Some(""), Some("")))
          result mustBe Left(Seq(FormError(bindKey, "error.twoRequired", Seq("month", "year"))))
        }
      }
      "day is invalid" which {
        "has a non-numeric value" in {
          val result = localDateFormatter().bind(bindKey, input(Some("q"), Some("10"), Some("2025")))
          result mustBe Left(Seq(FormError(s"$bindKey-dateDay", "error.invalid", Seq("day"))))
        }
        "is not a valid day" in {
          val result = localDateFormatter().bind(bindKey, input(Some("32"), Some("10"), Some("2025")))
          result mustBe Left(Seq(FormError(s"$bindKey-dateDay", "error.invalid", Seq("day"))))
        }
      }
      "month is invalid" which {
        "has a non-numeric value" in {
          val result = localDateFormatter().bind(bindKey, input(Some("15"), Some("asd"), Some("2025")))
          result mustBe Left(Seq(FormError(s"$bindKey-dateMonth", "error.invalid", Seq("month"))))
        }
        "is not a valid month" in {
          val result = localDateFormatter().bind(bindKey, input(Some("15"), Some("13"), Some("2025")))
          result mustBe Left(Seq(FormError(s"$bindKey-dateMonth", "error.invalid", Seq("month"))))
        }
      }
      "year is invalid" which {
        "has a non-numeric value" in {
          val result = localDateFormatter().bind(bindKey, input(Some("31"), Some("1"), Some("2o25")))
          result mustBe Left(Seq(FormError(s"$bindKey-dateYear", "error.invalidYear", Seq("year"))))
        }
        "is 3 digits" in {
          val result = localDateFormatter().bind(bindKey, input(Some("31"), Some("1"), Some("202")))
          result mustBe Left(Seq(FormError(s"$bindKey-dateYear", "error.invalidYear", Seq("year"))))
        }
        "is 5 digits" in {
          val result = localDateFormatter().bind(bindKey, input(Some("31"), Some("1"), Some("20251")))
          result mustBe Left(Seq(FormError(s"$bindKey-dateYear", "error.invalidYear", Seq("year"))))
        }
      }
    }
  }

  "unbind" should {
    "return a map with the date fields" in {
      val result = localDateFormatter().unbind(bindKey, LocalDate.of(2025, 1, 31))
      result mustBe Map(
        s"$bindKey-dateDay" -> "31",
        s"$bindKey-dateMonth" -> "1",
        s"$bindKey-dateYear" -> "2025"
      )
    }
  }

}
