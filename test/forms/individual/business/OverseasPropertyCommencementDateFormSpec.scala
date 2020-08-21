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

import forms.individual.business.OverseasPropertyCommencementDateForm.{overseasPropertyCommencementDateForm, startDate}
import forms.submapping.DateMapping._
import forms.validation.testutils.DataMap.DataMap
import models.DateModel
import models.individual.business.OverseasPropertyCommencementDateModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.{Form, FormError}


class OverseasPropertyCommencementDateFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  def form: Form[OverseasPropertyCommencementDateModel] = {
    overseasPropertyCommencementDateForm(OverseasPropertyCommencementDateForm.overseasPropertyStartDate.toString)
  }

  "The ForeignPropertyCommencementDateForm" should {
    "transform a valid request to the date form case class" in {
      val testDateDay = "31"
      val testDateMonth = "05"
      val testDateYear = "2017"
      val testInput = Map(
        s"$startDate.$dateDay" -> testDateDay, s"$startDate.$dateMonth" -> testDateMonth, s"$startDate.$dateYear" -> testDateYear
      )
      val expected = OverseasPropertyCommencementDateModel(
        DateModel(testDateDay, testDateMonth, testDateYear)
      )
      val actual = form.bind(testInput).value
      actual shouldBe Some(expected)
    }
    "when testing the validation" should {
      "output the appropriate error messages for the start date" when {
        val empty = "foreign.property.error.date.empty"
        val invalid = "foreign.property.error.date.empty"
        val afterMax = "foreign.property.error.property_commencement_date.minStartDate"

        "the date is not supplied to the map" in {
          form.bind(DataMap.EmptyMap).errors must contain(FormError(startDate, empty))
        }
        "the date supplied is empty" in {
          form.bind(DataMap.emptyDate(startDate)).errors must contain(FormError(startDate, empty))
        }
        "it is an invalid date" in {
          val invalidTest = form.bind(DataMap.date(startDate)("29", "2", "2017"))
          invalidTest.errors must contain(FormError(startDate, invalid))
        }
        "it is not older than 1 year ago" in {
          val currentDate = LocalDate.now()
          val testDate = currentDate.minusDays(364)
          val maxTest = form.bind(DataMap.date(startDate)(testDate.getDayOfMonth.toString, testDate.getMonthValue.toString, testDate.getYear.toString))
          maxTest.errors must contain(FormError(startDate, afterMax, Seq(currentDate.minusYears(1).toString)))
        }
        "it is missing the day" in {
          val dayTest = form.bind(DataMap.date(startDate)("", "4", "2017"))
          dayTest.errors must contain(FormError(startDate, "foreign.property.error.day.empty"))
        }
        "it is is missing the month" in {
          val mothTest = form.bind(DataMap.date(startDate)("06", "", "2017"))
          mothTest.errors must contain(FormError(startDate, "foreign.property.error.month.empty"))
        }
        "it is is missing the year" in {
          val yearTest = form.bind(DataMap.date(startDate)("06", "4", ""))
          yearTest.errors must contain(FormError(startDate, "foreign.property.error.year.empty"))
        }
        "it is is missing the day and month" in {
          val dayAndMonthTest = form.bind(DataMap.date(startDate)("", "", "2017"))
          dayAndMonthTest.errors must contain(FormError(startDate, "foreign.property.error.day.month.empty"))
        }
        "it is is missing the day and year" in {
          val dayAndYearTest = form.bind(DataMap.date(startDate)("", "4", ""))
          dayAndYearTest.errors must contain(FormError(startDate, "foreign.property.error.day.year.empty"))
        }
        "it is is missing the month and year" in {
          val monthAndYearTest = form.bind(DataMap.date(startDate)("06", "", ""))
          monthAndYearTest.errors must contain(FormError(startDate, "foreign.property.error.month.year.empty"))
        }
      }
    }
    "accept a valid date" in {
      val testData = DataMap.date(startDate)("28", "5", "2018")
      val validated = form.bind(testData)
      validated.hasErrors shouldBe false
      validated.hasGlobalErrors shouldBe false
    }
  }
}
