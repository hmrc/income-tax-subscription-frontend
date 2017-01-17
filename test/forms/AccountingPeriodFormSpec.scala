/*
 * Copyright 2017 HM Revenue & Customs
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

package forms

import assets.MessageLookup
import forms.submapping.DateMapping
import forms.validation.ErrorMessageFactory
import forms.validation.testutils.{DataMap, _}
import models.{AccountingPeriodModel, DateModel}
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class AccountingPeriodFormSpec extends PlaySpec with OneAppPerTest {

  import AccountingPeriodForm._
  import DateMapping._

  "The DateForm" should {
    "transform a valid request to the date form case class" in {
      val testDateDay = "31"
      val testDateMonth = "05"
      val testDateYear = "2017"

      val testDateDay2 = "01"
      val testDateMonth2 = "06"
      val testDateYear2 = "2018"

      val testInput = Map(
        startDate * dateDay -> testDateDay, startDate * dateMonth -> testDateMonth, startDate * dateYear -> testDateYear,
        endDate * dateDay -> testDateDay2, endDate * dateMonth -> testDateMonth2, endDate * dateYear -> testDateYear2
      )

      val expected = AccountingPeriodModel(
        DateModel(testDateDay, testDateMonth, testDateYear),
        DateModel(testDateDay2, testDateMonth2, testDateYear2))

      val actual = accountingPeriodForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "when testing the validation" should {

      "output the appropriate error messages for the start date" when {

        "a date is not supplied" in {
          val empty = ErrorMessageFactory.error("error.date.empty")
          empty fieldErrorIs MessageLookup.Error.Date.empty
          empty summaryErrorIs MessageLookup.Error.Date.empty

          accountingPeriodForm.bind(DataMap.EmptyMap) assert startDate hasExpectedErrors empty
          accountingPeriodForm.bind(DataMap.emptyDate(startDate)) assert startDate hasExpectedErrors empty
        }

        "it is an invalid date" in {
          val invalid = ErrorMessageFactory.error("error.date.invalid")
          invalid fieldErrorIs MessageLookup.Error.Date.invalid
          invalid summaryErrorIs MessageLookup.Error.Date.invalid

          val invalidTest = accountingPeriodForm.bind(DataMap.date(startDate)("29", "2", "2017"))
          invalidTest assert startDate hasExpectedErrors invalid
        }

        "it is before the 1 April 2017" in {
          val beforeMin = ErrorMessageFactory.error("error.business_accounting_period.minStartDate")
          beforeMin fieldErrorIs MessageLookup.Error.BusinessAccountingPeriod.minStartDate
          beforeMin summaryErrorIs MessageLookup.Error.BusinessAccountingPeriod.minStartDate

          val beforeMinTest = accountingPeriodForm.bind(DataMap.date(startDate)("01", "2", "2017"))
          beforeMinTest assert startDate hasExpectedErrors beforeMin
        }
      }

      "output an appropriate error for the end date" when {

        "a date is not supplied" in {
          val empty = ErrorMessageFactory.error("error.date.empty")
          empty fieldErrorIs MessageLookup.Error.Date.empty
          empty summaryErrorIs MessageLookup.Error.Date.empty

          val emptyDateInput0 = DataMap.EmptyMap
          val emptyTest0 = accountingPeriodForm.bind(emptyDateInput0)
          emptyTest0 assert endDate hasExpectedErrors empty

          val emptyDateInput = DataMap.emptyDate(endDate)
          val emptyTest = accountingPeriodForm.bind(emptyDateInput)
          emptyTest assert endDate hasExpectedErrors empty

        }

        "it is an invalid date" in {
          val invalid = ErrorMessageFactory.error("error.date.invalid")
          invalid fieldErrorIs MessageLookup.Error.Date.invalid
          invalid summaryErrorIs MessageLookup.Error.Date.invalid

          val invalidDateInput = DataMap.date(endDate)("29", "2", "2017")
          val invalidTest = accountingPeriodForm.bind(invalidDateInput)
          invalidTest assert endDate hasExpectedErrors invalid
        }

        "it is before the start date" in {
          val violation = ErrorMessageFactory.error("error.end_date_violation")
          violation fieldErrorIs MessageLookup.Error.Date.end_violation
          violation summaryErrorIs MessageLookup.Error.Date.end_violation

          val endDateViolationInput = DataMap.date(startDate)("28", "6", "2017") ++ DataMap.date(endDate)("28", "6", "2017")
          val violationTest = accountingPeriodForm.bind(endDateViolationInput)
          violationTest assert endDate hasExpectedErrors violation
        }

        "it is more than 24 months after the start date" in {
          val violation = ErrorMessageFactory.error("error.business_accounting_period.maxEndDate", "28 June 2019", "24")
          violation fieldErrorIs MessageLookup.Error.BusinessAccountingPeriod.maxEndDate("24", "28 June 2019")
          violation summaryErrorIs MessageLookup.Error.BusinessAccountingPeriod.maxEndDate("24", "28 June 2019")

          val endDateViolationInput = DataMap.date(startDate)("28", "6", "2017") ++ DataMap.date(endDate)("29", "6", "2019")
          val violationTest = accountingPeriodForm.bind(endDateViolationInput)
          violationTest assert endDate hasExpectedErrors violation
        }
      }
    }

    "accept a valid data" in {
      val testData = DataMap.date(startDate)("28", "5", "2017") ++ DataMap.date(endDate)("28", "5", "2018")
      accountingPeriodForm isValidFor testData
    }
  }

}
