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
    "transform the request to the form case class" in {
      val testDateDay = "01"
      val testDateMonth = "02"
      val testDateYear = "2000"

      val testDateDay2 = "31"
      val testDateMonth2 = "03"
      val testDateYear2 = "2001"

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

    "start date should be correctly validated" in {
      val empty = ErrorMessageFactory.error("error.date.empty")
      val invalid = ErrorMessageFactory.error("error.date.invalid")

      empty fieldErrorIs MessageLookup.Error.Date.empty
      empty summaryErrorIs MessageLookup.Error.Date.empty

      invalid fieldErrorIs MessageLookup.Error.Date.invalid
      invalid summaryErrorIs MessageLookup.Error.Date.invalid

      val emptyDateInput0 = DataMap.EmptyMap
      val emptyTest0 = accountingPeriodForm.bind(emptyDateInput0)
      emptyTest0 assert startDate hasExpectedErrors empty

      val emptyDateInput = DataMap.emptyDate(startDate)
      val emptyTest = accountingPeriodForm.bind(emptyDateInput)
      emptyTest assert startDate hasExpectedErrors empty

      val invalidDateInput = DataMap.date(startDate)("30", "2", "2017")
      val invalidTest = accountingPeriodForm.bind(invalidDateInput)
      invalidTest assert startDate hasExpectedErrors invalid
    }

    "end date should be correctly validated" in {
      val empty = ErrorMessageFactory.error("error.date.empty")
      val invalid = ErrorMessageFactory.error("error.date.invalid")
      val violation = ErrorMessageFactory.error("error.end_date_violation")

      empty fieldErrorIs MessageLookup.Error.Date.empty
      empty summaryErrorIs MessageLookup.Error.Date.empty

      invalid fieldErrorIs MessageLookup.Error.Date.invalid
      invalid summaryErrorIs MessageLookup.Error.Date.invalid

      violation fieldErrorIs MessageLookup.Error.end_date_violation
      violation summaryErrorIs MessageLookup.Error.end_date_violation

      val emptyDateInput0 = DataMap.EmptyMap
      val emptyTest0 = accountingPeriodForm.bind(emptyDateInput0)
      emptyTest0 assert endDate hasExpectedErrors empty

      val emptyDateInput = DataMap.emptyDate(endDate)
      val emptyTest = accountingPeriodForm.bind(emptyDateInput)
      emptyTest assert endDate hasExpectedErrors empty

      val invalidDateInput = DataMap.date(endDate)("29", "2", "2017")
      val invalidTest = accountingPeriodForm.bind(invalidDateInput)
      invalidTest assert endDate hasExpectedErrors invalid

      val endDateViolationInput = DataMap.date(startDate)("28", "2", "2017") ++ DataMap.date(endDate)("28", "2", "2017")
      val violationTest = accountingPeriodForm.bind(endDateViolationInput)
      violationTest assert endDate hasExpectedErrors violation

      val validInput = DataMap.date(startDate)("27", "2", "2017") ++ DataMap.date(endDate)("28", "2", "2017")
      val validTest = accountingPeriodForm.bind(validInput)
      validTest assert endDate doesNotHaveSpecifiedErrors violation
    }

    "The following submission should be valid" in {
      val testData = DataMap.date(startDate)("28", "2", "2017") ++ DataMap.date(endDate)("28", "2", "2018")
      accountingPeriodForm isValidFor testData
    }
  }

}
