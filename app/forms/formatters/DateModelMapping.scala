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

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._
import models.DateModel
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FormError, Mapping}

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

object DateModelMapping {
  val day: String = "dateDay"
  val month: String = "dateMonth"
  val year: String = "dateYear"

  final case class InvalidFields(fields: List[String])
  final case class EmptyFields(fields: List[String])
  final case class WrongLengthFields(fields: List[String])

  final case class Day(value: Int)
  final case class Month(value: Int)
  final case class Year(value: Int)

  final case class ValidDate(day: Day, month: Month, year: Year)

  case class DateModelFormatter(isAgent: Boolean,
                                errorContext: String,
                                minDate: Option[LocalDate],
                                maxDate: Option[LocalDate],
                                dateFormatter: Option[LocalDate => String])
    extends Formatter[DateModel] {

    def errorKey(error: String): String = if (isAgent) s"agent.error.$errorContext.$error" else s"error.$errorContext.$error"

    type DateModelValidation = Either[Seq[FormError], DateModel]
    type DateFormValidation[A] = ValidatedNel[String, A]

    private def parse(s: String): Option[Int] = Try {
      s.toInt
    }.toOption

    private def getValue(data: Map[String, String], key: String, fieldName: String): DateFormValidation[String] = {
      Validated.fromOption(getNonEmptyValueOption(data, key), NonEmptyList.of(s"$fieldName.empty"))
    }

    private def validNumber(value: String, fieldName: String): DateFormValidation[Int] = {
      Validated.fromOption(parse(value), NonEmptyList.of(s"$fieldName.invalid"))
    }

    private def validDay(value: Int): DateFormValidation[Int] = {
      if (value >= 1 && value <= 31) {
        value.valid
      } else {
        Invalid(NonEmptyList.of("day.invalid"))
      }
    }

    private def validMonth(value: Int): DateFormValidation[Int] = {
      if (value >= 1 && value <= 12) {
        value.valid
      } else {
        Invalid(NonEmptyList.of("month.invalid"))
      }
    }

    private def validYear(value: Int): DateFormValidation[Int] = {
      if (value >= 1000 && value <= 9999) {
        value.valid
      } else {
        Invalid(NonEmptyList.of("year.length"))
      }
    }

    private def validateDay(data: Map[String, String], ids: HtmlIds): DateFormValidation[Day] = {
      getValue(data, ids.totalDayKey, "day")
        .andThen(day => validNumber(day, "day"))
        .andThen(day => validDay(day))
        .map(day => Day(day))
    }

    private def validateMonth(data: Map[String, String], ids: HtmlIds): DateFormValidation[Month]  = {
      getValue(data, ids.totalMonthKey, "month")
        .andThen(month => validNumber(month, "month"))
        .andThen(month => validMonth(month))
        .map(month => Month(month))
    }

    private def validateYear(data: Map[String, String], ids: HtmlIds): DateFormValidation[Year] = {
      getValue(data, ids.totalYearKey, "year")
        .andThen(year => validNumber(year, "year"))
        .andThen(year => validYear(year))
        .map(year => Year(year))
    }

    private def validDayOfMonth(date: ValidDate): DateFormValidation[ValidDate] = {
      if (date.day.value > maxDayForMonth(date.month.value, date.year.value)) {
        Invalid(NonEmptyList.of("day.invalid"))
      } else {
        date.valid
      }
    }

    private def isValidDate(date: ValidDate, ids: HtmlIds): DateFormValidation[DateModel] = {
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

    private def validateDate(date: ValidDate, ids: HtmlIds): DateFormValidation[DateModel] = {
      validDayOfMonth(date).andThen(date => isValidDate(date, ids))
    }

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateModel] = {
      val ids: HtmlIds = HtmlIds(key)

      val result = (validateDay(data, ids), validateMonth(data, ids), validateYear(data, ids))
        .map3(
          (day, month, year) => ValidDate(day, month, year)
        ).andThen(date => validateDate(date, ids))

      result match {
        case Valid(date) => Right(date)
        case Invalid(errors) => transformErrors(errors.toList, ids)
      }
    }

    private def transformErrors(errors: List[String], ids: HtmlIds): DateModelValidation = {
      (mapToInvalidError(errors), mapToEmptyError(errors), mapToYearError(errors)) match {
        case (None, None, None) => {
          mapToDateError(errors, ids)
        }
        case (None, None, Some(WrongLengthFields(fields))) => {
          error(fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.length")
        }
        case (None, Some(EmptyFields(fields)), None) => {
          error(fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.empty")
        }
        case (None, Some(EmptyFields(emptyFields)), Some(WrongLengthFields(wrongLengthFields))) => {
          val fields = emptyFields ++ wrongLengthFields
          error(fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
        }
        case (Some(InvalidFields(fields)), None, None) => {
          error(fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
        }
        case (Some(InvalidFields(invalidFields)), None, Some(WrongLengthFields(wrongLengthFields))) => {
          val fields = invalidFields ++ wrongLengthFields
          error(fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
        }
        case (Some(InvalidFields(invalidFields)), Some(EmptyFields(emptyFields)), None) => {
          val fields = invalidFields ++ emptyFields
          error(fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(invalidFields ++ emptyFields)}.invalid")
        }
        case (Some(InvalidFields(invalidFields)), Some(EmptyFields(emptyFields)), Some(WrongLengthFields(wrongLengthFields))) => {
          val fields = invalidFields ++ emptyFields ++ wrongLengthFields
          error(fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
        }
      }
    }

    private def fieldsToFormKey(fields: List[String], ids: HtmlIds) = {
      (fields.contains("day"), fields.contains("month"), fields.contains("year")) match {
        case (true, _, _) => ids.totalDayKey
        case (false, true, _) => ids.totalMonthKey
        case (false, false, true) => ids.totalYearKey
        case _ => ids.totalDayKey
      }
    }

    private def fieldsToMessageKey(fields: List[String]): String =
      fields.toSet.toList.sorted.mkString("_")

    private def mapToInvalidError(errors: List[String]): Option[InvalidFields] = {
      val fields = collectFields(errors.contains("day.invalid"), errors.contains("month.invalid"), errors.contains("year.invalid"))
      if(fields.isEmpty) None else Some(InvalidFields(fields))
    }

    private def mapToEmptyError(errors: List[String]): Option[EmptyFields] = {
      val fields = collectFields(errors.contains("day.empty"), errors.contains("month.empty"), errors.contains("year.empty"))
      if(fields.isEmpty) None else Some(EmptyFields(fields))
    }

    private def collectFields(containsDay: Boolean, containsMonth: Boolean, containsYear: Boolean): List[String] = {
      (containsDay, containsMonth, containsYear) match {
        case (false, false, true) => List("year")
        case (false, true, false) => List("month")
        case (false, true, true) => List("month", "year")
        case (true, false, false) => List("day")
        case (true, false, true) => List("day", "year")
        case (true, true, false) => List("day", "month")
        case (true, true, true) => List("day", "month", "year")
        case _ => List.empty[String]
      }
    }

    private def mapToYearError(errors: List[String]): Option[WrongLengthFields] = {
      if(errors.contains("year.length")) {
        Some(WrongLengthFields(List("year")))
      } else {
        None
      }
    }

    private def mapToDateError(errors: List[String], ids: HtmlIds): DateModelValidation = {
      val formatter: LocalDate => String = dateFormatter.getOrElse(defaultDateFormatter)
      if(errors.contains("date.minDate")) {
        error(
          ids.totalDayKey,
          "day_month_year.minDate",
          minDate.fold(List.empty[String])(value => List(formatter(value)))
        )
      } else if(errors.contains("date.maxDate")) {
        error(
          ids.totalDayKey,
          "day_month_year.maxDate",
          maxDate.fold(List.empty[String])(value => List(formatter(value)))
        )
      } else {
        error(ids.totalDayKey, "day_month_year.empty")
      }
    }

    private def defaultDateFormatter: LocalDate => String = d => d.toString

    private def getNonEmptyValueOption(data: Map[String, String], id: String) = data.get(id).filter(_.nonEmpty)

    override def unbind(key: String, value: DateModel): Map[String, String] = {
      val ids = HtmlIds(key)
      Map(
        ids.totalDayKey -> value.day,
        ids.totalMonthKey -> value.month,
        ids.totalYearKey -> value.year
      )
    }

    private def error(id: String, errorKeyName: String, args: List[Any] = Nil) = Left(Seq(FormError(id, errorKey(errorKeyName), args)))
  }


  def dateModelMapping(isAgent: Boolean = false,
                       errorContext: String,
                       minDate: Option[LocalDate] = None,
                       maxDate: Option[LocalDate] = None,
                       dateFormatter: Option[LocalDate => String]): Mapping[DateModel] = of[DateModel](
    DateModelFormatter(
      isAgent = isAgent,
      errorContext = errorContext,
      minDate = minDate,
      maxDate = maxDate,
      dateFormatter = dateFormatter)
  )

  // Encapsulation of field ids.
  // We should never send people to the field with id "key".  Only to an editable field.
  // The sole exception to this is the fallback position where all our checks have passed
  // but the values do not parse to a local date for some reason.
  case class HtmlIds(key: String) {
    private val day: String = "dateDay"
    private val month: String = "dateMonth"
    private val year: String = "dateYear"

    val totalDayKey: String = s"$key-$day"
    val totalMonthKey: String = s"$key-$month"
    val totalYearKey: String = s"$key-$year"
  }

  private[formatters] def maxDayForMonth(month: Int, year: Int) = {
    month match {
      case 2 if (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0) => 29
      case 2 => 28
      case 4 | 6 | 9 | 11 => 30
      case _ => 31 // For 1 | 3 | 5 | 7 | 8 | 10 | 12
    }
  }

}

