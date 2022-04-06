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

import models.DateModel
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FormError, Mapping}

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

object DateModelMapping {

  private val invalid = "invalid"
  private val yearLength = "year.length"

  val day: String = "dateDay"
  val month: String = "dateMonth"
  val year: String = "dateYear"

  def parse(s: String): Option[Int] = Try {
    s.toInt
  }.toOption

  case class DateModelFormatter(isAgent: Boolean,
                                errorContext: String,
                                minDate: Option[LocalDate],
                                maxDate: Option[LocalDate],
                                dateFormatter: Option[LocalDate => String])
    extends Formatter[DateModel] {

    def errorKey(error: String): String = if (isAgent) s"agent.error.$errorContext.$error" else s"error.$errorContext.$error"

    type StringValidation = Either[Seq[FormError], (String, String, String)]
    type DateValidation = Either[Seq[FormError], (Int, Int, Int)]

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateModel] = {
      val ids = HtmlIds(key)
      val maybeDay: Option[String] = getNonEmptyValueOption(data, ids.totalDayKey)
      val maybeMonth: Option[String] = getNonEmptyValueOption(data, ids.totalMonthKey)
      val maybeYear: Option[String] = getNonEmptyValueOption(data, ids.totalYearKey)

      validateFieldsFilled(ids, maybeDay, maybeMonth, maybeYear) match {
        case Right((d, m, y)) => validateDate(ids)(Right((d, m, y))) match {
          case Right(_) => Right(DateModel(d, m, y))
          case Left(x) => Left(x)
        }
        case Left(x) => Left(x)
      }
    }

    private def validateFieldsFilled(
                                      ids: HtmlIds,
                                      maybeDay: Option[String],
                                      maybeMonth: Option[String],
                                      maybeYear: Option[String]): StringValidation = {
      (maybeDay, maybeMonth, maybeYear) match {
        case (None, None, None) => error(ids.totalDayKey, "date.empty")
        case (Some(_), None, None) => error(ids.totalMonthKey, "month_year.empty")
        case (None, Some(_), None) => error(ids.totalDayKey, "day_year.empty")
        case (Some(_), Some(_), None) => error(ids.totalYearKey, "year.empty")
        case (None, None, Some(_)) => error(ids.totalDayKey, "day_month.empty")
        case (Some(_), None, Some(_)) => error(ids.totalMonthKey, "month.empty")
        case (None, Some(_), Some(_)) => error(ids.totalDayKey, "day.empty")
        case (Some(dayValue), Some(monthValue), Some(yearValue)) => Right((dayValue, monthValue, yearValue))
      }
    }

    def checkForParsableToSaneValues(d: StringValidation)(implicit ids: HtmlIds): DateValidation = d match {
      case Right((d, m, y)) => (parse(d), parse(m), parse(y)) match {
        case (None, _, _) => error(ids.totalDayKey, invalid)
        case (_, None, _) => error(ids.totalMonthKey, invalid)
        case (_, _, None) => error(ids.totalYearKey, invalid)
        case (_, _, _) if y.length != 4 => error(ids.totalYearKey, yearLength)
        case (Some(dd), Some(mm), Some(yy)) => Right((dd, mm, yy))
      }
      case Left(x) => Left(x)
    }

    def checkForBadRawNumbers(d: DateValidation)(implicit ids: HtmlIds): DateValidation = d match {
      case Right((day, _, _)) if day > 31 || day < 1 => error(ids.totalDayKey, invalid)
      case Right((_, month, _)) if month > 12 || month < 1 => error(ids.totalMonthKey, invalid)
      case x => x
    }

    def checkForBadDayInMonth(d: DateValidation)(implicit ids: HtmlIds): DateValidation = d match {
      case Right((day, month, year)) if day > maxDayForMonth(month, year)
      => error(ids.totalDayKey, invalid)
      case x => x
    }

    def checkForValidDate(d: DateValidation)(implicit ids: HtmlIds): DateValidation = d match {
      case Right((day, month, year)) => Try {
        LocalDate.of(year, month, day)
      } match {
        case Failure(_) => error(ids.key, invalid)
        case Success(date) => (date, minDate, maxDate) match {
          case (d, Some(min), _) if d.isBefore(min) =>
            error(ids.totalDayKey, "start_date.minStartDate", List(dateFormatter.getOrElse(defaultDateFormatter)(min)))
          case (d, _, Some(max)) if d.isAfter(max) =>
            error(ids.totalDayKey, "start_date.maxStartDate", List(dateFormatter.getOrElse(defaultDateFormatter)(max)))
          case _ => d
        }
      }
      case x => x
    }

    private def defaultDateFormatter: LocalDate => String = d => d.toString

    private def validateDate(implicit ids: HtmlIds) =
      (checkForParsableToSaneValues _
        andThen checkForBadRawNumbers
        andThen checkForBadDayInMonth
        andThen checkForValidDate)

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
      case 1 | 3 | 5 | 7 | 8 | 10 | 12 => 31
      case _ => Integer.MAX_VALUE
    }
  }

}

