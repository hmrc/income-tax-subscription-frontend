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

package forms.agent

import forms.agent.PropertyStartDateForm.propertyStartDateForm
import forms.formatters.DateModelMapping
import models.DateModel
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}

import java.time.LocalDate

class PropertyStartDateFormSpec extends PlaySpec {

  val minStartDate: LocalDate = PropertyStartDateForm.minStartDate
  val maxStartDate: LocalDate = PropertyStartDateForm.maxStartDate
  val errorContext: String = "agent.error.property"

  def form: Form[DateModel] = {
    propertyStartDateForm(minStartDate, maxStartDate, _.toString)
  }

  def boundForm(day: String, month: String, year: String): Form[DateModel] = {
    form.bind(Map(
      s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}" -> day,
      s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}" -> month,
      s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}" -> year
    ))
  }

  "propertyStartDateForm" should {
    "return a date model" when {
      "a minimum possible date is provided" in {
        val date: DateModel = DateModel.dateConvert(minStartDate)

        boundForm(day = date.day, month = date.month, year = date.year)
          .value mustBe Some(date)
      }
      "a maximum possible date is provided" in {
        val date: DateModel = DateModel.dateConvert(maxStartDate)

        boundForm(day = date.day, month = date.month, year = date.year)
          .value mustBe Some(date)
      }
    }
    "return a form error" when {
      "a date before the minimum possible date is provided" in {
        val date: DateModel = DateModel.dateConvert(minStartDate.minusDays(1))
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}",
          message = s"$errorContext.day-month-year.min-date",
          args = Seq(minStartDate.toString)
        )

        boundForm(day = date.day, month = date.month, year = date.year)
          .errors mustBe Seq(expectedError)
      }
      "a date after the maximum possible date is provided" in {
        val date: DateModel = DateModel.dateConvert(maxStartDate.plusDays(1))
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}",
          message = s"$errorContext.day-month-year.max-date",
          args = Seq(maxStartDate.toString)
        )

        boundForm(day = date.day, month = date.month, year = date.year)
          .errors mustBe Seq(expectedError)
      }
      "a date missing it's day field is provided" in {
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}",
          message = s"$errorContext.day.empty"
        )

        boundForm(day = "", month = "1", year = "2020")
          .errors mustBe Seq(expectedError)
      }
      "a date missing it's month field is provided" in {
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}",
          message = s"$errorContext.month.empty"
        )

        boundForm(day = "1", month = "", year = "2020")
          .errors mustBe Seq(expectedError)
      }
      "a date missing it's year field is provided" in {
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}",
          message = s"$errorContext.year.empty"
        )

        boundForm(day = "1", month = "1", year = "")
          .errors mustBe Seq(expectedError)
      }
      "a date missing it's day and month fields is provided" in {
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}",
          message = s"$errorContext.day-month.empty"
        )

        boundForm(day = "", month = "", year = "2020")
          .errors mustBe Seq(expectedError)
      }
      "a date missing it's month and year fields is provided" in {
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}",
          message = s"$errorContext.month-year.empty"
        )

        boundForm(day = "1", month = "", year = "")
          .errors mustBe Seq(expectedError)
      }
      "a date missing it's day, month and year fields is provided" in {
        val expectedError: FormError = FormError(
          key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}",
          message = s"$errorContext.day-month-year.empty"
        )

        boundForm(day = "", month = "", year = "")
          .errors mustBe Seq(expectedError)
      }
    }
  }

}
