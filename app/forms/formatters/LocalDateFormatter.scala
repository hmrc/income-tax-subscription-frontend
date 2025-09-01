/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.data.FormError
import play.api.data.format.Formatter

import java.time.{LocalDate, Month}
import scala.util.{Failure, Success, Try}

private[formatters] class LocalDateFormatter(
                                              invalidKey: String,
                                              allRequiredKey: String,
                                              twoRequiredKey: String,
                                              requiredKey: String,
                                              invalidYearKey: String,
                                              args: Seq[String] = Seq.empty
                                            ) extends Formatter[LocalDate] with Formatters {

  private val fieldKeys: List[String] = List("Day", "Month", "Year")

  private def yearLengthIsValid(year: Option[String]): Boolean = year.exists(_.length == 4) && year.exists(_.forall(_.isDigit))
  private def monthLengthIsValid(month: String): Boolean = if (month.forall(_.isDigit)) month.toInt <= 12 else true
  private def dayLengthIsValid(day: String): Boolean = day match {
      case day if day.forall(_.isDigit) => 0 < day.toInt && day.toInt <= 31
      case _ => false
    }

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] = {
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_) =>
        Left(Seq(FormError(key, invalidKey, args)))
    }
  }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    def int(args: Seq[String] = args) = intFormatter(
      requiredKey = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey = invalidKey,
      args
    )

    val month = new MonthFormatter(invalidKey, Seq("month"))

    for {
      day <- int(Seq("day")).bind(s"$key-dateDay", data)
      month <- month.bind(s"$key-dateMonth", data)
      year <- int(Seq("year")).bind(s"$key-dateYear", data)
      date <- toDate(key, day, month, year)
    } yield date

  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields: Map[String, Option[String]] = fieldKeys.map {
      field =>
        field -> data.get(s"$key-date$field").filter(_.nonEmpty)
    }.toMap

    lazy val missingFields: Seq[String] = fields
      .withFilter(_._2.isEmpty)
      .map(_._1)
      .toList

    fields.count(_._2.isDefined) match {
      case 3 =>
        val lengthErrors = fields.collect {
          case("Day", value) if !dayLengthIsValid(value.getOrElse("")) => FormError(s"$key-dateDay", invalidKey, Seq("day"))
          case("Month", value) if !monthLengthIsValid(value.getOrElse("")) => FormError(s"$key-dateMonth", invalidKey, Seq("month"))
          case("Year", value) if !yearLengthIsValid(value) => FormError(s"$key-dateYear", invalidYearKey, Seq("year"))
        }.toList

        if (lengthErrors.nonEmpty) {
          if (lengthErrors.length > 1) {
            Left(List(FormError(key, invalidKey)))
          } else {
            Left(lengthErrors)
          }
        } else {
          formatDate(key, data)
        }

      case 2 =>
        Left(List(FormError(key, s"$requiredKey", missingFields.map(_.toLowerCase) ++ args)))
      case 1 =>
        Left(List(FormError(key, twoRequiredKey, missingFields.map(_.toLowerCase) ++ args)))
      case _ =>
        Left(List(FormError(key, allRequiredKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key-dateDay" -> value.getDayOfMonth.toString,
      s"$key-dateMonth" -> value.getMonthValue.toString,
      s"$key-dateYear" -> value.getYear.toString
    )
}

private class MonthFormatter(invalidKey: String, args: Seq[String]) extends Formatter[Int] with Formatters {

  private val baseFormatter = stringFormatter(invalidKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {

    val months = Month.values.toList

    baseFormatter
      .bind(key, data)
      .flatMap {
        str =>
          months
            .find(m => m.getValue.toString == str.replaceAll("^0+", "") || m.toString == str.toUpperCase || m.toString.take(3) == str.toUpperCase)
            .map(x => Right(x.getValue))
            .getOrElse{
              Left(List(FormError(key, invalidKey, args)))}
      }
  }

  override def unbind(key: String, value: Int): Map[String, String] =
    Map(key -> value.toString)
}

