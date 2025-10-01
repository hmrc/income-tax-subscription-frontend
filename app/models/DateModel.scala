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

package models

import java.time.format.{DateTimeFormatter, ResolverStyle}
import java.time.{LocalDate, ZoneId}
import java.util.Date
import com.ibm.icu.text.SimpleDateFormat
import models.DateModel._
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

case class DateModel(day: String, month: String, year: String) {

  def toLocalDate: LocalDate = LocalDate.of(year.toInt, month.toInt, day.toInt)

  def plusDays(days: Int): DateModel = DateModel.dateConvert(this.toLocalDate.plusDays(days))

  def plusYears(years: Int): DateModel = DateModel.dateConvert(this.toLocalDate.plusYears(years))

  def toOutputDateFormat: String = toLocalDate.format(outputFormat)

  def toCheckYourAnswersDateFormat(implicit messages: Messages): String = {
    val sdf = new SimpleDateFormat("d MMMM yyyy", messages.lang.locale)

    val date = Date.from(toLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant)

    sdf.format(date)
  }

  def toDesDateFormat: String = toLocalDate.format(desFormat)

  def diffInMonth(that: DateModel): Int = {
    import java.time.temporal.ChronoUnit
    ChronoUnit.MONTHS.between(dateConvert(this), dateConvert(that)).toInt
  }

  def matches(dateModel: DateModel): Boolean = {
    this.toLocalDate == dateModel.toLocalDate
  }
}

object DateModel {

  type DateModelValidation = Either[Seq[FormError], DateModel]

  val outputFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu").withResolverStyle(ResolverStyle.STRICT)

  val desFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)

  def dateConvert(date: DateModel): LocalDate = date.toLocalDate

  def dateConvert(date: LocalDate): DateModel = DateModel(date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)

  implicit val format: OFormat[DateModel] = Json.format[DateModel]

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
