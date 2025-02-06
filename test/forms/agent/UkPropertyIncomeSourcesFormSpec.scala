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

  val form: Form[(YesNo, AccountingMethod)] = ukPropertyIncomeSourcesForm

  "ukPropertyIncomeSourceFormNoDate" should {
    "bind successfully" when {
      "a start date before limit answer 'Yes' and an accounting method 'Cash' is provided" in {
        val testInput = Map(
          startDateBeforeLimit -> YesNoMapping.option_yes,
          accountingMethodProperty -> AccountingMethodMapping.option_cash
        )
        val expected = (Yes, Cash)
        val actual = form.bind(testInput).value

        actual mustBe Some(expected)
      }
      "a start date before limit answer 'No' and an accounting method 'Accruals' is provided" in {
        val testInput = Map(
          startDateBeforeLimit -> YesNoMapping.option_no,
          accountingMethodProperty -> AccountingMethodMapping.option_accruals
        )
        val expected = (No, Accruals)
        val actual = form.bind(testInput).value

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

        form.bind(testInput).errors mustBe Seq(expectedError)
      }
      "accounting method is missing" in {
        val testInput = Map(
          startDateBeforeLimit -> YesNoMapping.option_yes
        )
        val expectedError: FormError = FormError(
          key = accountingMethodProperty,
          message = s"agent.error.accounting-method-property.invalid"
        )

        form.bind(testInput).errors mustBe Seq(expectedError)
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

        form.bind(testInput).errors mustBe expectedErrors
      }
    }
  }

}
