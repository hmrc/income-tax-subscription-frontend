/*
 * Copyright 2024 HM Revenue & Customs
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

import forms.agent.UkPropertyIncomeSourcesForm._
import forms.submapping.{AccountingMethodMapping, YesNoMapping}
import models._
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}
import utilities.AccountingPeriodUtil

import java.time.LocalDate

class UkPropertyIncomeSourcesFormSpec extends PlaySpec {
  def dateFormatter(date: LocalDate): String = date.toString

  val form: Form[(DateModel, AccountingMethod)] = ukPropertyIncomeSourcesForm(_.toString)
  val formNoDate: Form[(YesNo, AccountingMethod)] = ukPropertyIncomeSourcesFormNoDate

  "ukPropertyIncomeSourceFormNoDate" should {
    "bind successfully" when {
      "a start date before limit answer 'Yes' and an accounting method 'Cash' is provided" in {
        val testInput = Map(
          startDateBeforeLimit -> YesNoMapping.option_yes,
          accountingMethodProperty -> AccountingMethodMapping.option_cash
        )
        val expected = (Yes, Cash)
        val actual = formNoDate.bind(testInput).value

        actual mustBe Some(expected)
      }
      "a start date before limit answer 'No' and an accounting method 'Accruals' is provided" in {
        val testInput = Map(
          startDateBeforeLimit -> YesNoMapping.option_no,
          accountingMethodProperty -> AccountingMethodMapping.option_accruals
        )
        val expected = (No, Accruals)
        val actual = formNoDate.bind(testInput).value

        actual mustBe Some(expected)
      }
    }
    "fail to bind" when {
      "start date before limit is missing" in {
        val testInput = Map(
          accountingMethodProperty -> AccountingMethodMapping.option_cash
        )
        val expectedError: FormError = FormError(
          key = startDateBeforeLimit,
          message = s"agent.error.$errorContext.income-source.$startDateBeforeLimit.invalid",
          args = Seq(AccountingPeriodUtil.getStartDateLimit.getYear.toString)
        )

        formNoDate.bind(testInput).errors mustBe Seq(expectedError)
      }
      "accounting method is missing" in {
        val testInput = Map(
          startDateBeforeLimit -> YesNoMapping.option_yes
        )
        val expectedError: FormError = FormError(
          key = accountingMethodProperty,
          message = s"agent.error.accounting-method-property.invalid"
        )

        formNoDate.bind(testInput).errors mustBe Seq(expectedError)
      }
      "both fields are missing" in {
        val testInput = Map.empty[String, String]
        val expectedErrors: Seq[FormError] = Seq(
          FormError(
            key = startDateBeforeLimit,
            message = s"agent.error.$errorContext.income-source.$startDateBeforeLimit.invalid",
            args = Seq(AccountingPeriodUtil.getStartDateLimit.getYear.toString)
          ),
          FormError(
            key = accountingMethodProperty,
            message = s"agent.error.accounting-method-property.invalid"
          )
        )

        formNoDate.bind(testInput).errors mustBe expectedErrors
      }
    }
  }

  "UkPropertyIncomeSourcesForm" should {

    "bind valid data" in {
      val date = DateModel("10", "6", "2023")
      val testInput = Map(
        s"$startDate-dateDay" -> date.day,
        s"$startDate-dateMonth" -> date.month,
        s"$startDate-dateYear" -> date.year,
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      )
      val expected = (date, Cash)
      val actual = form.bind(testInput).value

      actual mustBe Some(expected)
    }

    "fail to bind when date is missing" in {
      val testInput = Map(
        s"$startDate-dateDay" -> "",
        s"$startDate-dateMonth" -> "",
        s"$startDate-dateYear" -> "",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day-month-year.empty"))
    }

    "fail to bind when date is out of bounds (too early)" in {
      val testInput = Map(
        s"$startDate-dateDay" -> "31",
        s"$startDate-dateMonth" -> "12",
        s"$startDate-dateYear" -> "1899",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day-month-year.min-date", Seq("1900-01-01")))
    }

    "fail to bind when date is out of bounds (too late)" in {
      val maxDate = LocalDate.now().plusDays(7)
      val testInput = Map(
        s"$startDate-dateDay" -> maxDate.getDayOfMonth.toString,
        s"$startDate-dateMonth" -> maxDate.getMonthValue.toString,
        s"$startDate-dateYear" -> maxDate.getYear.toString,
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day-month-year.max-date", Seq(maxDate.minusDays(1).toString)))
    }

    "unbind data correctly" in {
      val filledForm = form.fill((DateModel("10", "6", "2023"), Cash))

      filledForm.data must contain allOf(
        s"$startDate-dateDay" -> "10",
        s"$startDate-dateMonth" -> "6",
        s"$startDate-dateYear" -> "2023",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      )
    }

    "show an error when date is not supplied" in {
      val result = form.bind(Map.empty[String, String])
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day-month-year.empty"))
    }

    "show an error when date is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "31",
        s"$startDate-dateMonth" -> "13",
        s"$startDate-dateYear" -> "1899",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateMonth", "agent.error.property.month.invalid"))
    }

    "show an error when day is missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "",
        s"$startDate-dateMonth" -> "4",
        s"$startDate-dateYear" -> "2017",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day.empty"))
    }

    "show an error when month is missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "",
        s"$startDate-dateYear" -> "2017",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateMonth", "agent.error.property.month.empty"))
    }

    "show an error when year is missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "1",
        s"$startDate-dateYear" -> "",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.property.year.empty"))
    }

    "show an error when multiple fields are missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "",
        s"$startDate-dateMonth" -> "",
        s"$startDate-dateYear" -> "2017",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day-month.empty"))
    }

    "show an error when day is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "0",
        s"$startDate-dateMonth" -> "1",
        s"$startDate-dateYear" -> "2017",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day.invalid"))
    }

    "show an error when month is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "13",
        s"$startDate-dateYear" -> "2017",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateMonth", "agent.error.property.month.invalid"))
    }

    "show an error when year is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "1",
        s"$startDate-dateYear" -> "invalid",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.property.year.invalid"))
    }

    "show an error when multiple fields are invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "0",
        s"$startDate-dateMonth" -> "0",
        s"$startDate-dateYear" -> "2017",
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.property.day-month.invalid"))
    }

    "show an error when year length is incorrect" when {
      "year has 3 digits" in {
        val result = form.bind(Map(
          s"$startDate-dateDay" -> "1",
          s"$startDate-dateMonth" -> "1",
          s"$startDate-dateYear" -> "123",
          accountingMethodProperty -> AccountingMethodMapping.option_cash
        ))
        result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.property.year.length"))
      }

      "year has 5 digits" in {
        val result = form.bind(Map(
          s"$startDate-dateDay" -> "1",
          s"$startDate-dateMonth" -> "1",
          s"$startDate-dateYear" -> "12345",
          accountingMethodProperty -> AccountingMethodMapping.option_cash
        ))
        result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.property.year.length"))
      }
    }

    "fail to bind when Accounting Method is invalid" in {
      val testInput = Map(
        s"$startDate-dateDay" -> "10",
        s"$startDate-dateMonth" -> "6",
        s"$startDate-dateYear" -> "2023",
        accountingMethodProperty -> "invalid_method"
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(accountingMethodProperty, "agent.error.accounting-method-property.invalid"))
    }


  }

  "accept a valid date" when {
    "the date is 7 days ahead from current date" in {
      val sevenDaysInPast = LocalDate.now().plusDays(6)
      val testData = Map(
        s"$startDate-dateDay" -> sevenDaysInPast.getDayOfMonth.toString,
        s"$startDate-dateMonth" -> sevenDaysInPast.getMonthValue.toString,
        s"$startDate-dateYear" -> sevenDaysInPast.getYear.toString,
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      )
      val validated = form.bind(testData)
      validated.hasErrors mustBe false
      validated.hasGlobalErrors mustBe false
    }

    "the date is the 1 January 1900" in {
      val earliestAllowedDate = LocalDate.of(1900, 1, 1)
      val testData = Map(
        s"$startDate-dateDay" -> earliestAllowedDate.getDayOfMonth.toString,
        s"$startDate-dateMonth" -> earliestAllowedDate.getMonthValue.toString,
        s"$startDate-dateYear" -> earliestAllowedDate.getYear.toString,
        accountingMethodProperty -> AccountingMethodMapping.option_cash
      )
      val validated = form.bind(testData)
      validated.hasErrors mustBe false
      validated.hasGlobalErrors mustBe false
    }
  }


}
