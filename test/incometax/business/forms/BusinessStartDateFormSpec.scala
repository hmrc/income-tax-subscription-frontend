/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.business.forms

import assets.MessageLookup
import core.forms.submapping.DateMapping
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import core.models.DateModel
import incometax.business.models.BusinessStartDateModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages.Implicits._

class BusinessStartDateFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import DateMapping._
  import incometax.business.forms.BusinessStartDateForm._

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

        "a date is not supplied" in {
          val empty = ErrorMessageFactory.error("error.date.empty")
          empty fieldErrorIs MessageLookup.Error.Date.empty
          empty summaryErrorIs MessageLookup.Error.Date.empty

          businessStartDateForm.bind(DataMap.EmptyMap) assert startDate hasExpectedErrors empty
          businessStartDateForm.bind(DataMap.emptyDate(startDate)) assert startDate hasExpectedErrors empty
        }

        "it is an invalid date" in {
          val invalid = ErrorMessageFactory.error("error.date.invalid")
          invalid fieldErrorIs MessageLookup.Error.Date.invalid
          invalid summaryErrorIs MessageLookup.Error.Date.invalid

          val invalidTest = businessStartDateForm.bind(DataMap.date(startDate)("29", "2", "2017"))
          invalidTest assert startDate hasExpectedErrors invalid
        }

        "it is before the 6 April 2017" in {
          val beforeMin = ErrorMessageFactory.error("error.business_accounting_period.minStartDate")
          beforeMin fieldErrorIs MessageLookup.Error.BusinessAccountingPeriod.minStartDate
          beforeMin summaryErrorIs MessageLookup.Error.BusinessAccountingPeriod.minStartDate

          val beforeMinTest = businessStartDateForm.bind(DataMap.date(startDate)("05", "4", "2017"))
          beforeMinTest assert startDate hasExpectedErrors beforeMin

          val minTest = businessStartDateForm.bind(DataMap.date(startDate)("06", "4", "2017"))
          minTest assert startDate doesNotHaveSpecifiedErrors beforeMin
        }
      }
    }

    "accept a valid date" in {
      val testData = DataMap.date(startDate)("28", "5", "2017")
      businessStartDateForm isValidFor testData
    }
  }

}
