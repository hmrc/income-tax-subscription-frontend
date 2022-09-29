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
import cats.implicits._
import forms.formatters.DateValidation._
import models.DateModel
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FormError, Mapping}

import java.time.LocalDate

object DateModelMapping {
  val day: String = "dateDay"
  val month: String = "dateMonth"
  val year: String = "dateYear"

  type DateModelValidation = Either[Seq[FormError], DateModel]

  case class DateModelFormatter(isAgent: Boolean,
                                errorContext: String,
                                minDate: Option[LocalDate],
                                maxDate: Option[LocalDate],
                                dateFormatter: Option[LocalDate => String])
    extends Formatter[DateModel] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateModel] = {
      val ids: HtmlIds = HtmlIds(key)

      val result = ((validateDay(data, ids), validateMonth(data, ids))
        .mapN(
          (day, month) => DayMonth(day, month)
        ).andThen(validateDayMonth), validateYear(data, ids))
        .mapN(
          (dayMonth, year) => ValidDate(dayMonth.day, dayMonth.month, year)
        ).andThen(date => validateDate(date, minDate, maxDate))

      result match {
        case Valid(date) => Right(date)
        case Invalid(errors) =>
          DateErrorMapping.transformErrors(errors.toList, ids, isAgent, errorContext, minDate, maxDate, dateFormatter)
      }
    }

    override def unbind(key: String, value: DateModel): Map[String, String] = {
      val ids = HtmlIds(key)
      Map(
        ids.totalDayKey -> value.day,
        ids.totalMonthKey -> value.month,
        ids.totalYearKey -> value.year
      )
    }
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
}
