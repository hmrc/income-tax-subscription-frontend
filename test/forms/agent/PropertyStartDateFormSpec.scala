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

import forms.agent.PropertyStartDateForm.{propertyStartDateForm, startDate}
import forms.formatters.DateModelMapping.{day, month, year}
import forms.validation.testutils.DataMap.DataMap
import models.DateModel
import models.common.PropertyStartDateModel
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}

import java.time.LocalDate

class PropertyStartDateFormSpec extends PlaySpec {

  def form: Form[DateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate, PropertyStartDateForm.maxStartDate, d => d.toString)
  }

  "The PropertyStartDateForm" should {
    "transform a valid request to the date form case class" in {
      val testDateDay = "31"
      val testDateMonth = "5"
      val testDateYear = "2017"
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

        val errorContext: String = "agent.error.property"

        "the date is not supplied to the map" in {
          form.bind(DataMap.EmptyMap).errors must contain(FormError(dayKeyError, s"$errorContext.day-month-year.empty"))
        }
        "the date supplied to the map is invalid" in {
          form.bind(DataMap.govukDate(startDate)("31", "13", "1899")).errors must contain(FormError(monthKeyError, s"$errorContext.month.invalid"))
        }
        "it is not within 7 days from current date" in {
          val sevenDaysInFuture: LocalDate = LocalDate.now.plusDays(7)
          val maxTest = form.bind(DataMap.govukDate(startDate)(
            sevenDaysInFuture.getDayOfMonth.toString,
            sevenDaysInFuture.getMonthValue.toString,
            sevenDaysInFuture.getYear.toString
          ))
          maxTest.errors must contain(FormError(dayKeyError, s"$errorContext.day-month-year.max-date", List(PropertyStartDateForm.maxStartDate.toString)))
        }
        "it is before year 1900" in {
          val minTest = form.bind(DataMap.govukDate(startDate)("31", "12", "1899"))
          minTest.errors must contain(FormError(dayKeyError, s"$errorContext.day-month-year.min-date", List(PropertyStartDateForm.minStartDate.toString)))
        }
        "it is missing the day" in {
          val test = form.bind(DataMap.govukDate(startDate)("", "4", "2017"))
          test.errors must contain(FormError(dayKeyError, s"$errorContext.day.empty"))
        }
        "it is missing the month" in {
          val test = form.bind(DataMap.govukDate(startDate)("1", "", "2017"))
          test.errors must contain(FormError(monthKeyError, s"$errorContext.month.empty"))
        }
        "it is missing the year" in {
          val test = form.bind(DataMap.govukDate(startDate)("1", "1", ""))
          test.errors must contain(FormError(yearKeyError, s"$errorContext.year.empty"))
        }
        "it is missing multiple fields" in {
          val test = form.bind(DataMap.govukDate(startDate)("", "", "2017"))
          test.errors must contain(FormError(dayKeyError, s"$errorContext.day-month.empty"))
        }
        "it has an invalid day" in {
          val test = form.bind(DataMap.govukDate(startDate)("0", "1", "2017"))
          test.errors must contain(FormError(dayKeyError, s"$errorContext.day.invalid"))
        }
        "it has an invalid month" in {
          val test = form.bind(DataMap.govukDate(startDate)("1", "13", "2017"))
          test.errors must contain(FormError(monthKeyError, s"$errorContext.month.invalid"))
        }
        "it has an invalid year" in {
          val test = form.bind(DataMap.govukDate(startDate)("1", "1", "invalid"))
          test.errors must contain(FormError(yearKeyError, s"$errorContext.year.invalid"))
        }
        "it has multiple invalid fields" in {
          val test = form.bind(DataMap.govukDate(startDate)("0", "0", "2017"))
          test.errors must contain(FormError(dayKeyError, s"$errorContext.day-month.invalid"))
        }
        "the year provided is not the correct length" when {
          "the year is 3 digits" in {
            val test = form.bind(DataMap.govukDate(startDate)("1", "1", "123"))
            test.errors must contain(FormError(yearKeyError, s"$errorContext.year.length"))
          }
          "the year is 5 digits" in {
            val test = form.bind(DataMap.govukDate(startDate)("1", "1", "12345"))
            test.errors must contain(FormError(yearKeyError, s"$errorContext.year.length"))
          }
        }
      }
    }
    "accept a valid date" when {
      "the date is 7 days ahead from current date" in {
        val sevenDaysInPast: LocalDate = LocalDate.now.plusDays(6)
        val testData = DataMap.govukDate(startDate)(
          day = sevenDaysInPast.getDayOfMonth.toString,
          month = sevenDaysInPast.getMonthValue.toString,
          year = sevenDaysInPast.getYear.toString
        )
        val validated = form.bind(testData)
        validated.hasErrors mustBe false
        validated.hasGlobalErrors mustBe false
      }
      "the date is the first of january 1900" in {
        val earliestAllowedDate: LocalDate = LocalDate.of(1900, 1, 1)
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
