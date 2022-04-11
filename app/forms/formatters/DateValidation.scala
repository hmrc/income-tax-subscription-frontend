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

import cats.data.Validated.Invalid
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._
import forms.formatters.DateModelMapping._
import models.DateModel

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

object DateValidation {
  type DateFormValidation[A] = ValidatedNel[String, A]

  final case class Day(value: Int)
  final case class Month(value: Int)
  final case class Year(value: Int)

  final case class ValidDate(day: Day, month: Month, year: Year)

  def validateDay(data: Map[String, String], ids: HtmlIds): DateFormValidation[Day] = {
    getValue(data, ids.totalDayKey, "day")
      .andThen(day => validNumber(day, "day"))
      .andThen(day => isValidDay(day))
      .map(day => Day(day))
  }

  def validateMonth(data: Map[String, String], ids: HtmlIds): DateFormValidation[Month]  = {
    getValue(data, ids.totalMonthKey, "month")
      .andThen(month => validNumber(month, "month"))
      .andThen(month => isValidMonth(month))
      .map(month => Month(month))
  }

  def validateYear(data: Map[String, String], ids: HtmlIds): DateFormValidation[Year] = {
    getValue(data, ids.totalYearKey, "year")
      .andThen(year => validNumber(year, "year"))
      .andThen(year => isValidYear(year))
      .map(year => Year(year))
  }

  def validateDate(date: ValidDate, ids: HtmlIds, minDate: Option[LocalDate] = None, maxDate: Option[LocalDate] = None): DateFormValidation[DateModel] = {
    isValidDayOfMonth(date).andThen(date => isValidDate(date, minDate, maxDate))
  }

  private def getValue(data: Map[String, String], key: String, fieldName: String): DateFormValidation[String] = {
    Validated.fromOption(getNonEmptyValueOption(data, key), NonEmptyList.of(s"$fieldName.empty"))
  }

  private def validNumber(value: String, fieldName: String): DateFormValidation[Int] = {
    Validated.fromOption(parse(value), NonEmptyList.of(s"$fieldName.invalid"))
  }

  private def isValidDay(value: Int): DateFormValidation[Int] = {
    if (value >= 1 && value <= 31) {
      value.valid
    } else {
      Invalid(NonEmptyList.of("day.invalid"))
    }
  }

  private def isValidMonth(value: Int): DateFormValidation[Int] = {
    if (value >= 1 && value <= 12) {
      value.valid
    } else {
      Invalid(NonEmptyList.of("month.invalid"))
    }
  }

  private def isValidYear(value: Int): DateFormValidation[Int] = {
    if (value >= 1000 && value <= 9999) {
      value.valid
    } else {
      Invalid(NonEmptyList.of("year.length"))
    }
  }

  private def isValidDayOfMonth(date: ValidDate): DateFormValidation[ValidDate] = {
    if (date.day.value > maxDayForMonth(date.month.value, date.year.value)) {
      Invalid(NonEmptyList.of("day.invalid"))
    } else {
      date.valid
    }
  }

  private def isValidDate(date: ValidDate,
                          minDate: Option[LocalDate] = None,
                          maxDate: Option[LocalDate] = None
                         ): DateFormValidation[DateModel] = {
    val yearValue = date.year.value
    val monthValue = date.month.value
    val dayValue = date.day.value
    Try {
      LocalDate.of(yearValue, monthValue, dayValue)
    } match {
      case Failure(_) => Invalid(NonEmptyList.of("date.invalid"))
      case Success(localDate) => (localDate, minDate, maxDate) match {
        case (d, Some(min), _) if d.isBefore(min) =>
          Invalid(NonEmptyList.of("date.minDate"))
        case (d, _, Some(max)) if d.isAfter(max) =>
          Invalid(NonEmptyList.of("date.maxDate"))
        case _ => DateModel(dayValue.toString, monthValue.toString, yearValue.toString).valid
      }
    }
  }

  private def getNonEmptyValueOption(data: Map[String, String], id: String) = data.get(id).filter(_.nonEmpty)

  private def parse(s: String): Option[Int] = Try {
    s.toInt
  }.toOption

  private[formatters] def maxDayForMonth(month: Int, year: Int) = {
    month match {
      case 2 if (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0) => 29
      case 2 => 28
      case 4 | 6 | 9 | 11 => 30
      case _ => 31 // For 1 | 3 | 5 | 7 | 8 | 10 | 12
    }
  }
}
