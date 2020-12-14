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

package forms.individual.business

import java.time.LocalDate

import forms.individual.business.PropertyStartDateForm.{propertyStartDateForm, startDate}
import forms.formatters.DateModelMapping.{day, month, year}
import forms.validation.testutils.DataMap.DataMap
import models.DateModel
import models.common.PropertyStartDateModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.{Form, FormError}


class PropertyStartDateFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  def form: Form[PropertyStartDateModel] = {
    propertyStartDateForm(PropertyStartDateForm.minStartDate.toString, PropertyStartDateForm.maxStartDate.toString)
  }

  "The PropertyStartDateForm" should {
    "transform a valid request to the date form case class" in {
      val testDateDay = "31"
      val testDateMonth = "05"
      val testDateYear = "2017"
      val testInput = Map(
        s"$startDate.$day" -> testDateDay, s"$startDate.$month" -> testDateMonth, s"$startDate.$year" -> testDateYear
      )
      val expected = PropertyStartDateModel(
        DateModel(testDateDay, testDateMonth, testDateYear)
      )
      val actual = form.bind(testInput).value
      actual shouldBe Some(expected)
    }
    "when testing the validation" should {
      "output the appropriate error messages for the start date" when {
        val empty = "error.property.date.empty"
        val invalid = "error.property.date.empty"
        val afterMax = "error.property.start_date.maxStartDate"
        val beforeMin = "error.property.start_date.minStartDate"

        val dayKeyError: String = s"$startDate.$day"
        val monthKeyError: String = s"$startDate.$month"
        val yearKeyError: String = s"$startDate.$year"

        "the date is not supplied to the map" in {
          form.bind(DataMap.EmptyMap).errors must contain(FormError(dayKeyError, empty))
        }
        "the date supplied is empty" in {
          form.bind(DataMap.emptyDate(startDate)).errors must contain(FormError(dayKeyError, empty))
        }
        "it is an invalid date" in {
          val invalidTest = form.bind(DataMap.date(startDate)("29", "2", "2017"))
          invalidTest.errors must contain(FormError(startDate, invalid))
        }
        "it is within 1 years" in {
          val oneYearAgo: LocalDate = LocalDate.now.minusMonths(6)
          val maxTest = form.bind(DataMap.date(startDate)(
            oneYearAgo.getDayOfMonth.toString,
            oneYearAgo.getMonthValue.toString,
            oneYearAgo.getYear.toString
          ))
          maxTest.errors must contain(FormError(startDate, afterMax, Seq(PropertyStartDateForm.maxStartDate.toString)))
        }
        "it is before year 1900" in {
          val minTest = form.bind(DataMap.date(startDate)("31", "12", "1899"))
          minTest.errors must contain(FormError(startDate, beforeMin, Seq(PropertyStartDateForm.minStartDate.toString)))
        }
        "it is missing the day" in {
          val dayTest = form.bind(DataMap.date(startDate)("", "4", "2017"))
          dayTest.errors must contain(FormError(dayKeyError, "error.property.day.empty"))
        }
        "it is is missing the month" in {
          val mothTest = form.bind(DataMap.date(startDate)("06", "", "2017"))
          mothTest.errors must contain(FormError(monthKeyError, "error.property.month.empty"))
        }
        "it is is missing the year" in {
          val yearTest = form.bind(DataMap.date(startDate)("06", "4", ""))
          yearTest.errors must contain(FormError(yearKeyError, "error.property.year.empty"))
        }
        "it is is missing the day and month" in {
          val dayAndMonthTest = form.bind(DataMap.date(startDate)("", "", "2017"))
          dayAndMonthTest.errors must contain(FormError(dayKeyError, "error.property.day_month.empty"))
        }
        "it is is missing the day and year" in {
          val dayAndYearTest = form.bind(DataMap.date(startDate)("", "4", ""))
          dayAndYearTest.errors must contain(FormError(dayKeyError, "error.property.day_year.empty"))
        }
        "it is is missing the month and year" in {
          val monthAndYearTest = form.bind(DataMap.date(startDate)("06", "", ""))
          monthAndYearTest.errors must contain(FormError(monthKeyError, "error.property.month_year.empty"))
        }
      }
    }
    "accept a valid date" when {
      "the date is exactly one years ago" in {
        val oneYearAgo: LocalDate = LocalDate.now.minusYears(1)
        val testData = DataMap.date(startDate)(
          day = oneYearAgo.getDayOfMonth.toString,
          month = oneYearAgo.getMonthValue.toString,
          year = oneYearAgo.getYear.toString
        )
        val validated = form.bind(testData)
        validated.hasErrors shouldBe false
        validated.hasGlobalErrors shouldBe false
      }
      "the date is the first of january 1900" in {
        val earliestAllowedDate: LocalDate = LocalDate.of(1900, 1, 1)
        val testData = DataMap.date(startDate)(
          day = earliestAllowedDate.getDayOfMonth.toString,
          month = earliestAllowedDate.getMonthValue.toString,
          year = earliestAllowedDate.getYear.toString
        )
        val validated = form.bind(testData)
        validated.hasErrors shouldBe false
        validated.hasGlobalErrors shouldBe false
      }
    }
  }
}