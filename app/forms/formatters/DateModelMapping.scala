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
import scala.util.Try

object DateModelMapping {

  val day: String = "dateDay"
  val month: String = "dateMonth"
  val year: String = "dateYear"


  def isDayValid(dayText: String, monthText: String, yearText: String): Either[String, Int] = {
    Try[Either[String, Int]] {
      val day: Int = dayText.toInt
      val month: Int = monthText.toInt
      val year: Int = yearText.toInt

      val dayMaxValid: Boolean = month match {
        case 2 if year % 4 == 0 => day <= 29
        case 2 => day <= 28
        case 4 | 6 | 9 | 11 => day <= 30
        case 1 | 3 | 5 | 7 | 8 | 10 | 12 => day <= 31
      }

      if (day >= 1 && dayMaxValid) Right(day) else Left("invalid")

    }.getOrElse(Left("invalid"))
  }

  def isMonthValid(monthText: String): Either[String, Int] = {
    Try[Either[String, Int]] {
      val month = monthText.toInt
      if (month >= 1 && month <= 12) {
        Right(month)
      } else {
        Left("invalid")
      }
    }.getOrElse(Left("invalid"))
  }

  def isYearValid(yearText: String): Either[String, Int] = {
    Try[Either[String, Int]] {
      Right(yearText.toInt)
    }.getOrElse(Left("invalid"))
  }

  def dateModelFormatter(isAgent: Boolean = false, errorContext: String): Formatter[DateModel] = new Formatter[DateModel] {

    def errorKey(error: String): String = if (isAgent) s"agent.error.$errorContext.$error" else s"error.$errorContext.$error"

    def totalDayKey(key: String): String = s"$key.$day"

    def totalMonthKey(key: String): String = s"$key.$month"

    def totalYearKey(key: String): String = s"$key.$year"

    def validateFieldsFilled(
                              key: String, maybeDay: Option[String],
                              maybeMonth: Option[String],
                              maybeYear: Option[String]): Either[FormError, (String, String, String)] = {
      (maybeDay, maybeMonth, maybeYear) match {
        case (None, None, None) => Left(FormError(key, errorKey("date.empty")))
        case (Some(_), None, None) => Left(FormError(key, errorKey("month_year.empty")))
        case (None, Some(_), None) => Left(FormError(key, errorKey("day_year.empty")))
        case (None, None, Some(_)) => Left(FormError(key, errorKey("day_month.empty")))
        case (Some(_), Some(_), None) => Left(FormError(totalYearKey(key), errorKey("year.empty")))
        case (None, Some(_), Some(_)) => Left(FormError(totalDayKey(key), errorKey("day.empty")))
        case (Some(_), None, Some(_)) => Left(FormError(totalMonthKey(key), errorKey("month.empty")))
        case (Some(dayValue), Some(monthValue), Some(yearValue)) => Right((dayValue, monthValue, yearValue))
      }
    }

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateModel] = {
      val maybeDay: Option[String] = data.get(totalDayKey(key)).filter(_.nonEmpty)
      val maybeMonth: Option[String] = data.get(totalMonthKey(key)).filter(_.nonEmpty)
      val maybeYear: Option[String] = data.get(totalYearKey(key)).filter(_.nonEmpty)

      val formErrorOrDateValues = validateFieldsFilled(key, maybeDay, maybeMonth, maybeYear)

      formErrorOrDateValues match {
        case Left(value) => Left(Seq(value))
        case Right((day, month, year)) =>
          (isDayValid(day, month, year), isMonthValid(month), isYearValid(year)) match {
            case (Right(day), Right(month), Right(year)) => Try[Either[Seq[FormError], DateModel]] {
              LocalDate.of(year, month, day)
              Right(DateModel(maybeDay.get, maybeMonth.get, maybeYear.get))
            }.getOrElse(Left(Seq(FormError(key, errorKey("invalid")))))
            case (Left(_), Right(_), Right(_)) => Left(Seq(FormError(totalDayKey(key), errorKey("invalid"))))
            case (Right(_), Left(_), Right(_)) => Left(Seq(FormError(totalMonthKey(key), errorKey("invalid"))))
            case (Right(_), Right(_), Left(_)) => Left(Seq(FormError(totalYearKey(key), errorKey("invalid"))))
            case _ => Left(Seq(FormError(key, errorKey("invalid"))))
          }
      }
    }

    override def unbind(key: String, value: DateModel): Map[String, String] = Map(
      totalDayKey(key) -> value.day,
      totalMonthKey(key) -> value.month,
      totalYearKey(key) -> value.year
    )
  }

  def dateModelMapping(isAgent: Boolean = false, errorContext: String): Mapping[DateModel] = of[DateModel](
    dateModelFormatter(isAgent = isAgent, errorContext = errorContext))
}
