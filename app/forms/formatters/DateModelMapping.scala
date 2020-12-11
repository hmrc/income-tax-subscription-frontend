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

package forms.formatters

import forms.validation.utils.MappingUtil.{OTextUtil, oText}
import models.DateModel
import play.api.data.Forms.{mapping, of}
import play.api.data.format.Formatter
import play.api.data.{FormError, Mapping}

object DateModelMapping {

  val day: String = "dateDay"
  val month: String = "dateMonth"
  val year: String = "dateYear"

  val dateMapping: Mapping[DateModel] = mapping(
    day -> oText.toText,
    month -> oText.toText,
    year -> oText.toText
  )(DateModel.apply)(DateModel.unapply)

  def dateModelFormatter(isAgent: Boolean = false, errorContext: String): Formatter[DateModel] = new Formatter[DateModel] {

    def errorKey(error: String): String = if (isAgent) s"agent.error.$errorContext.$error.empty" else s"error.$errorContext.$error.empty"

    def totalDayKey(key: String): String = s"$key.$day"

    def totalMonthKey(key: String): String = s"$key.$month"

    def totalYearKey(key: String): String = s"$key.$year"

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateModel] = {
      val day: Option[String] = data.get(totalDayKey(key)).filter(_.nonEmpty)
      val month: Option[String] = data.get(totalMonthKey(key)).filter(_.nonEmpty)
      val year: Option[String] = data.get(totalYearKey(key)).filter(_.nonEmpty)

      (day, month, year) match {
        case (None, None, None) => Left(Seq(FormError(totalDayKey(key), errorKey("date"))))
        case (Some(_), None, None) => Left(Seq(FormError(totalMonthKey(key), errorKey("month_year"))))
        case (None, Some(_), None) => Left(Seq(FormError(totalDayKey(key), errorKey("day_year"))))
        case (None, None, Some(_)) => Left(Seq(FormError(totalDayKey(key), errorKey("day_month"))))
        case (Some(_), Some(_), None) => Left(Seq(FormError(totalYearKey(key), errorKey("year"))))
        case (None, Some(_), Some(_)) => Left(Seq(FormError(totalDayKey(key), errorKey("day"))))
        case (Some(_), None, Some(_)) => Left(Seq(FormError(totalMonthKey(key), errorKey("month"))))
        case (Some(dayValue), Some(monthValue), Some(yearValue)) => Right(DateModel(dayValue, monthValue, yearValue))
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
