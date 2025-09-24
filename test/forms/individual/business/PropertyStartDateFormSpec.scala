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
import forms.formatters.DateModelMapping.{day, month, year}
import forms.individual.business.PropertyStartDateForm.{propertyStartDateForm, startDate}
import forms.validation.testutils.DataMap.DataMap
import models.DateModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.{Form, FormError}
import utilities.AccountingPeriodUtil

import java.time.LocalDate


class PropertyStartDateFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  def form: Form[DateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate, PropertyStartDateForm.maxStartDate, d => d.toString)
  }

  "The PropertyStartDateForm" should {
    "transform a valid request to the date form case class" in {
      val testDateDay = "6"
      val testDateMonth = "4"
      val testDateYear = AccountingPeriodUtil.getStartDateLimit.getYear.toString
      val testInput = Map(
        s"$startDate-$day" -> testDateDay, s"$startDate-$month" -> testDateMonth, s"$startDate-$year" -> testDateYear
      )
      val expected = DateModel(testDateDay, testDateMonth, testDateYear)
      val actual = form.bind(testInput).value
      actual mustBe Some(expected)
    }
    "when testing the validation" should {
      "output the appropriate error messages for the start date" when {
        val dayKeyError: String = s"$startDate-$day"
        val monthKeyError: String = s"$startDate-$month"
        val yearKeyError: String = s"$startDate-$year"

        val errorContext: String = "error.property"

        "the date is not supplied to the map" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.empty",
            args = Seq()
          )
          form.bind(DataMap.EmptyMap).errors must contain(expectedError)
        }

        def boundForm(day: String, month: String, year: String): Form[DateModel] = {
          form.bind(Map(
            s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}" -> day,
            s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}" -> month,
            s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}" -> year
          ))
        }

        val minStartDate: LocalDate = PropertyStartDateForm.minStartDate
        val maxStartDate: LocalDate = PropertyStartDateForm.maxStartDate

        "it is not within 7 days from current day" in {
          val date: DateModel = DateModel.dateConvert(maxStartDate.plusDays(1))
          val expectedError: FormError = FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.day-month-year.max-date"
          )

          val error = boundForm(day = date.day, month = date.month, year = date.year)
            .errors.head

          error.copy(args = Seq.empty) mustBe expectedError
        }
        "it is before minimum date" in {
          val date: DateModel = DateModel.dateConvert(minStartDate.minusDays(1))
          val expectedError: FormError = FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.day-month-year.min-date"
          )

          val error = boundForm(day = date.day, month = date.month, year = date.year)
            .errors.head

          error.copy(args = Seq.empty) mustBe expectedError
        }
        "it is missing the day" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.required",
            args = Seq("day")
          )

          val test = form.bind(DataMap.govukDate(startDate)("", "4", "2017"))
          test.errors must contain(expectedError)
        }
        "it is missing the month" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.required",
            args = Seq("month")
          )
          val test = form.bind(DataMap.govukDate(startDate)("1", "", "2017"))
          test.errors must contain(expectedError)
        }
        "it is missing the year" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.required",
            args = Seq("year")
          )
          val test = form.bind(DataMap.govukDate(startDate)("1", "1", ""))
          test.errors must contain(expectedError)
        }
        "it is missing multiple fields" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.required.two",
            args = Seq("day","month")
          )
          val test = form.bind(DataMap.govukDate(startDate)("", "", "2017"))
          test.errors must contain(expectedError)
        }
        "it has an invalid day" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}",
            message = s"$errorContext.invalid",
            args = Seq("day")
          )
          val test = form.bind(DataMap.govukDate(startDate)("0", "1", "2017"))
          test.errors must contain(expectedError)
        }
        "it has an invalid month" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}",
            message = s"$errorContext.invalid",
            args = Seq("month")
          )
          val test = form.bind(DataMap.govukDate(startDate)("1", "13", "2017"))
          test.errors must contain(expectedError)
        }
        "it has an invalid year" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}",
            message = s"$errorContext.year.length",
            args = Seq("year")
          )
          val test = form.bind(DataMap.govukDate(startDate)("1", "1", "invalid"))
          test.errors must contain(expectedError)
        }
        "it has multiple invalid fields" in {
          val expectedError:FormError=FormError(
            key = s"${PropertyStartDateForm.startDate}",
            message = s"$errorContext.invalid",
            args = Seq()
          )
          val test = form.bind(DataMap.govukDate(startDate)("0", "0", "2017"))
          test.errors must contain(expectedError)
        }
        "the year provided is not the correct length" when {
          "the year is 3 digits" in {
            val expectedError:FormError=FormError(
              key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}",
              message = s"$errorContext.year.length",
              args = Seq("year")
            )
            val test = form.bind(DataMap.govukDate(startDate)("1", "1", "123"))
            println(test+"<---------------")
            test.errors must contain(expectedError)
          }
          "the year is 5 digits" in {
            val expectedError:FormError=FormError(
              key = s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}",
              message = s"$errorContext.year.length",
              args = Seq("year")
            )
            val test = form.bind(DataMap.govukDate(startDate)("1", "1", "12345"))
            test.errors must contain(expectedError)
          }
        }
      }
    }
    "accept a valid date" when {
      "the date is 7 days ahead from current date" in {
        val sevenDaysInPresent: LocalDate = LocalDate.now.plusDays(6)
        val testData = DataMap.govukDate(startDate)(
          day = sevenDaysInPresent.getDayOfMonth.toString,
          month = sevenDaysInPresent.getMonthValue.toString,
          year = sevenDaysInPresent.getYear.toString
        )
        val validated = form.bind(testData)
        validated.hasErrors mustBe false
        validated.hasGlobalErrors mustBe false
      }
      "the date is the limit date" in {
        val earliestAllowedDate: LocalDate = AccountingPeriodUtil.getStartDateLimit
        val testData = DataMap.govukDate(startDate)(
          day = earliestAllowedDate.getDayOfMonth.toString,
          month = earliestAllowedDate.getMonthValue.toString,
          year = earliestAllowedDate.getYear.toString
        )
        val validated = form.bind(testData)
        validated.hasErrors mustBe false
        validated.hasGlobalErrors mustBe false
      }
    }
  }
}
