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

import forms.submapping.DateMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.DateModel
import models.individual.business.BusinessStartDateModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError

class BusinessStartDateFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import DateMapping._
  import forms.individual.business.BusinessStartDateForm._

  "The BusinessStartDateForm" should {
    "transform a valid request to the date form case class" in {
      val testDateDay = "31"
      val testDateMonth = "05"
      val testDateYear = "2017"

      val testDateDay2 = "01"
      val testDateMonth2 = "06"
      val testDateYear2 = "2018"

      val testInput = Map(
        startDate * dateDay -> testDateDay, startDate * dateMonth -> testDateMonth, startDate * dateYear -> testDateYear
      )

      val expected = BusinessStartDateModel(
        DateModel(testDateDay, testDateMonth, testDateYear)
      )

      val actual = businessStartDateForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "when testing the validation" should {

      "output the appropriate error messages for the start date" when {
        val empty = "error.date.empty"
        val invalid = "error.date.invalid"
        val beforeMin = "error.business_accounting_period.minStartDate"

        "the date is not supplied to the map" in {
          businessStartDateForm.bind(DataMap.EmptyMap).errors must contain(FormError(startDate, empty))
        }

        "the date supplied is empty" in {
          businessStartDateForm.bind(DataMap.emptyDate(startDate)).errors must contain(FormError(startDate, empty))
        }

        "it is an invalid date" in {
          val invalidTest = businessStartDateForm.bind(DataMap.date(startDate)("29", "2", "2017"))
          invalidTest.errors must contain(FormError(startDate, invalid))
        }

        "if it is before the 6 April 2017" in {
          val beforeMinTest = businessStartDateForm.bind(DataMap.date(startDate)("05", "4", "2017"))
          beforeMinTest.errors must contain(FormError(startDate, beforeMin))
        }

        "if it is not before the 6 April 2017" in {
          val minTest = businessStartDateForm.bind(DataMap.date(startDate)("06", "4", "2017"))
          minTest.errors mustNot contain(FormError(startDate, beforeMin))
        }
      }
    }

    "accept a valid date" in {
      val testData = DataMap.date(startDate)("28", "5", "2017")
      businessStartDateForm isValidFor testData
    }
  }

}
