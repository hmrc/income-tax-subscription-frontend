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

package forms.individual.business

import forms.formatters.DateModelMapping
import forms.individual.business.ForeignPropertyStartDateForm.{errorContext, minStartDate, startDate, startDateForm}
import models.DateModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.{Form, FormError}
import utilities.AccountingPeriodUtil

import java.time.LocalDate

class ForeignPropertyStartDateFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  val dateDayKey: String = s"$startDate-${DateModelMapping.day}"
  val dateMonthKey: String = s"$startDate-${DateModelMapping.month}"
  val dateYearKey: String = s"$startDate-${DateModelMapping.year}"

  def boundForm(dateModel: DateModel): Form[DateModel] = {
    startDateForm(_.toString).bind(Map(
      dateDayKey -> dateModel.day,
      dateMonthKey -> dateModel.month,
      dateYearKey -> dateModel.year
    ))
  }

  val now: DateModel = DateModel.dateConvert(LocalDate.now)

  "ForeignPropertyStartDateForm" must {
    "return a valid date model" when {
      "a date is provided to the form" which {
        "is the minimum possible date" in {
          val date: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

          boundForm(date).value mustBe Some(date)
        }
        "is the maximum possible date" in {
          val date: DateModel = DateModel.dateConvert(LocalDate.now.plusDays(6))

          boundForm(date).value mustBe Some(date)
        }
      }
    }
    "return a form error" when {
      "no date fields are provided to the form" in {
        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.empty",
          args = Seq()
        )

        startDateForm(_.toString).bind(Map.empty[String, String])
          .errors mustBe Seq(expectedError)
      }

      "the day field is empty" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.required",
          args = Seq("day")
        )
        val date: DateModel = now.copy(day = "")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the month field is empty" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.required",
          args = Seq("month")
        )
        val date: DateModel = now.copy(month = "")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the year field is empty" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.required",
          args = Seq("year")
        )
        val date: DateModel = now.copy(year = "")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the day and month fields are empty" in {
        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.required.two",
          args = Seq("day","month")
        )
        val date: DateModel = now.copy(day = "", month = "")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the day and year fields are empty" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.required.two",
          args = Seq("day","year")
        )
        val date: DateModel = now.copy(day = "", year = "")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the month and year fields are empty" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.required.two",
          args = Seq("month","year")
        )
        val date: DateModel = now.copy(month = "", year = "")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "all date fields are empty" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.empty",
          args = Seq()
        )
        val date: DateModel = DateModel("", "", "")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }

      "the day field is invalid" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}-${DateModelMapping.day}",
          message = s"error.$errorContext.invalid",
          args = Seq("day")
        )
        val date: DateModel = now.copy(day = "32")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the month field is invalid" in {
        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}-${DateModelMapping.month}",
          message = s"error.$errorContext.invalid",
          args = Seq("month")
        )
        val date: DateModel = now.copy(month = "13")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the year field is invalid" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}-${DateModelMapping.year}",
          message = s"error.$errorContext.year.length",
          args = Seq("year")
        )
        val date: DateModel = now.copy(year = "invalid")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the day and month is invalid" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.invalid",
          args = Seq()
        )
        val date: DateModel = now.copy(day = "32", month = "13")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the day and year is invalid" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.invalid",
          args = Seq()
        )
        val date: DateModel = now.copy(day = "0", year = "invalid")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "the month and year is invalid" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.invalid",
          args = Seq()
        )
        val date: DateModel = now.copy(month = "13", year = "invalid")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }
      "all date fields are invalid" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.invalid",
          args = Seq()
        )
        val date: DateModel = now.copy(day = "32", month = "13", year = "invalid")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }

      "the date is not a real date" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.invalid",
          args = Seq()
        )
        val date: DateModel = now.copy(day = "30", month = "2")

        boundForm(date).errors mustBe Seq(
          expectedError
        )
      }

      "the year is not the correct length" when {
        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}-${DateModelMapping.year}",
          message = s"error.$errorContext.year.length",
          args = Seq("year")
        )

        "it's too short" in {


          val date: DateModel = now.copy(year = "999")

          boundForm(date).errors mustBe Seq(
            expectedError
          )
        }
        "it's too long" in {
          val date: DateModel = now.copy(year = "10000")

          boundForm(date).errors mustBe Seq(
            expectedError
          )
        }
      }

      "the date entered is too early" in {

        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"error.$errorContext.day-month-year.min-date",
        )
        val date: DateModel = DateModel.dateConvert(minStartDate.minusDays(1))
        val error=boundForm(date).errors.head
        error.copy(args=Seq.empty) mustBe expectedError
//        boundForm(date).errors mustBe Seq(
////          FormError(dateDayKey, s"error.$errorContext.day-month-year.min-date", Seq(AccountingPeriodUtil.getStartDateLimit.toString))
//            expectedError
//        )
      }

      "the date entered is too late" in {
        val expectedError:FormError=FormError(
          key = s"${ForeignPropertyStartDateForm.startDate}",
          message = s"agent.error.$errorContext.day-month-year.max-date",
        )
        val date: DateModel = DateModel.dateConvert(LocalDate.now.plusDays(7))

        val error=boundForm(date).errors.head
        error.copy(args=Seq.empty) mustBe expectedError

        boundForm(date).errors mustBe Seq(
            expectedError
        )
      }
    }
  }

}
