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

import forms.submapping.AccountingMethodMapping
import models.{AccountingMethod, Cash, DateModel}
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}
import IncomeSourcesOverseasPropertyForm.{accountingMethodOverseasProperty, _}

import java.time.LocalDate

class OverseasPropertyIncomeSourcesFormSpec extends PlaySpec {
  def dateFormatter(date: LocalDate): String = date.toString

  val form: Form[(DateModel, AccountingMethod)] = incomeSourcesOverseasPropertyForm(_.toString)

  "UkPropertyIncomeSourcesForm" should {

    "bind valid data" in {
      val date = DateModel("10", "6", "2023")
      val testInput = Map(
        s"$startDate-dateDay" -> date.day,
        s"$startDate-dateMonth" -> date.month,
        s"$startDate-dateYear" -> date.year,
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
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
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day-month-year.empty"))
    }

    "fail to bind when date is out of bounds (too early)" in {
      val testInput = Map(
        s"$startDate-dateDay" -> "31",
        s"$startDate-dateMonth" -> "12",
        s"$startDate-dateYear" -> "1899",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day-month-year.min-date", Seq("1900-01-01")))
    }

    "fail to bind when date is out of bounds (too late)" in {
      val maxDate = LocalDate.now().plusDays(7)
      val testInput = Map(
        s"$startDate-dateDay" -> maxDate.getDayOfMonth.toString,
        s"$startDate-dateMonth" -> maxDate.getMonthValue.toString,
        s"$startDate-dateYear" -> maxDate.getYear.toString,
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day-month-year.max-date", Seq(maxDate.minusDays(1).toString)))
    }

    "unbind data correctly" in {
      val filledForm = form.fill((DateModel("10", "6", "2023"), Cash))

      filledForm.data must contain allOf (
        s"$startDate-dateDay" -> "10",
        s"$startDate-dateMonth" -> "6",
        s"$startDate-dateYear" -> "2023",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      )
    }

    "show an error when date is not supplied" in {
      val result = form.bind(Map.empty[String, String])
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day-month-year.empty"))
    }

    "show an error when date is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "31",
        s"$startDate-dateMonth" -> "13",
        s"$startDate-dateYear" -> "1899",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateMonth", "agent.error.overseas.property.month.invalid"))
    }

    "show an error when day is missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "",
        s"$startDate-dateMonth" -> "4",
        s"$startDate-dateYear" -> "2017",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day.empty"))
    }

    "show an error when month is missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "",
        s"$startDate-dateYear" -> "2017",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateMonth", "agent.error.overseas.property.month.empty"))
    }

    "show an error when year is missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "1",
        s"$startDate-dateYear" -> "",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.overseas.property.year.empty"))
    }

    "show an error when multiple fields are missing" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "",
        s"$startDate-dateMonth" -> "",
        s"$startDate-dateYear" -> "2017",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day-month.empty"))
    }

    "show an error when day is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "0",
        s"$startDate-dateMonth" -> "1",
        s"$startDate-dateYear" -> "2017",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day.invalid"))
    }

    "show an error when month is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "13",
        s"$startDate-dateYear" -> "2017",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateMonth", "agent.error.overseas.property.month.invalid"))
    }

    "show an error when year is invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "1",
        s"$startDate-dateMonth" -> "1",
        s"$startDate-dateYear" -> "invalid",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.overseas.property.year.invalid"))
    }

    "show an error when multiple fields are invalid" in {
      val result = form.bind(Map(
        s"$startDate-dateDay" -> "0",
        s"$startDate-dateMonth" -> "0",
        s"$startDate-dateYear" -> "2017",
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      ))
      result.errors must contain(FormError(s"$startDate-dateDay", "agent.error.overseas.property.day-month.invalid"))
    }

    "show an error when year length is incorrect" when {
      "year has 3 digits" in {
        val result = form.bind(Map(
          s"$startDate-dateDay" -> "1",
          s"$startDate-dateMonth" -> "1",
          s"$startDate-dateYear" -> "123",
          accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
        ))
        result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.overseas.property.year.length"))
      }

      "year has 5 digits" in {
        val result = form.bind(Map(
          s"$startDate-dateDay" -> "1",
          s"$startDate-dateMonth" -> "1",
          s"$startDate-dateYear" -> "12345",
          accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
        ))
        result.errors must contain(FormError(s"$startDate-dateYear", "agent.error.overseas.property.year.length"))
      }
    }

    "fail to bind when Accounting Method is invalid" in {
      val testInput = Map(
        s"$startDate-dateDay" -> "10",
        s"$startDate-dateMonth" -> "6",
        s"$startDate-dateYear" -> "2023",
        accountingMethodOverseasProperty -> "invalid_method"
      )
      val result = form.bind(testInput)
      result.value mustBe None

      result.errors must contain(FormError(accountingMethodOverseasProperty, "agent.error.accounting-method-property.invalid"))
    }


  }

  "accept a valid date" when {
    "the date is 7 days ahead from current date" in {
      val sevenDaysInPast = LocalDate.now().plusDays(6)
      val testData = Map(
        s"$startDate-dateDay" -> sevenDaysInPast.getDayOfMonth.toString,
        s"$startDate-dateMonth" -> sevenDaysInPast.getMonthValue.toString,
        s"$startDate-dateYear" -> sevenDaysInPast.getYear.toString,
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
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
        accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash
      )
      val validated = form.bind(testData)
      validated.hasErrors mustBe false
      validated.hasGlobalErrors mustBe false
    }
  }



}
