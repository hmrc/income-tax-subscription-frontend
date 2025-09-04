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
import scala.util.control.Exception.nonFatalCatch
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

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] = {
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_) =>
        Left(Seq(FormError(key, invalidKey, args)))
    }
  }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    def dayLengthInvalid(day: Int): Boolean = day < 1 || day > 31
    def yearLengthInvalid(year: Int): Boolean = year.toString.length != 4

    val dayFormatter = new DayYearFormatter(requiredKey, invalidKey, invalidKey, additionalValidation = dayLengthInvalid, Seq("day"))
    val monthFormatter = new MonthFormatter(invalidKey, Seq("month"))
    val yearFormatter = new DayYearFormatter(requiredKey, invalidYearKey, invalidYearKey, additionalValidation = yearLengthInvalid, Seq("year"))

    val formattedFields = Map(
      "day" -> dayFormatter.bind(s"$key-dateDay", data),
      "month" -> monthFormatter.bind(s"$key-dateMonth", data),
      "year" -> yearFormatter.bind(s"$key-dateYear", data)
    )

    formattedFields.collect { case (_, Left(value)) => value } match {
      case Nil => for {
        day <- formattedFields("day")
        month <- formattedFields("month")
        year <- formattedFields("year")
        date <- toDate(key, day, month, year)
      } yield date
      case errors =>
        if (errors.size > 1) {
          Left(Seq(FormError(key, invalidKey)))
        } else {
          Left(errors.flatten.toSeq)
        }
    }
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
        formatDate(key, data)
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

private class MonthFormatter(invalidKey: String, args: Seq[String] = Seq.empty) extends Formatter[Int] with Formatters {

  private val baseFormatter = stringFormatter(invalidKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {

    val months = Month.values.toList

    baseFormatter
      .bind(key, data)
      .flatMap {
        str =>
          months
            .find(m => m.getValue.toString == str.trim.replaceAll("^0+|\\s+", "") || m.toString == str.toUpperCase || m.toString.take(3) == str.toUpperCase)
            .map(x => Right(x.getValue))
            .getOrElse {
              Left(List(FormError(key, invalidKey, args)))
            }
      }
  }

  override def unbind(key: String, value: Int): Map[String, String] =
    Map(key -> value.toString)
}

private class DayYearFormatter(requiredKey: String,
                               invalidKey: String,
                               validationKey: String,
                               additionalValidation: Int => Boolean = _ => false,
                               args: Seq[String] = Seq.empty) extends Formatter[Int] with Formatters {
  private val baseFormatter = stringFormatter(requiredKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {

    val baseFormatted = {
      baseFormatter
        .bind(key, data)
        .map(_.replaceAll("^0+|\\s+", ""))
        .flatMap { s =>
          nonFatalCatch
            .either(s.toInt)
            .left.map(_ => {
            Seq(FormError(key, invalidKey, args))
          })
        }
    }

    baseFormatted match {
      case Right(value) if additionalValidation(value) => Left(Seq(FormError(key, validationKey, args)))
      case _ => baseFormatted
    }
  }

  override def unbind(key: String, value: Int): Map[String, String] =
    baseFormatter.unbind(key, value.toString)
}


