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
import forms.validation.ErrorMessageFactory
import forms.validation.testutils.{DataMap, _}
import models.RegisterNextAccountingPeriodModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._
import RegisterNextAccountingPeriodForm._

class RegisterNextAccountingPeriodFormSpec extends PlaySpec with OneAppPerTest {

  "The RegisterNextAccountingPeriodForm" should {
    "transform the request to the form case class" in {
      val testIncomeSource = option_yes
      val testInput = Map(registerNextAccountingPeriod -> testIncomeSource)
      val expected = RegisterNextAccountingPeriodModel(testIncomeSource)
      val actual = registerNextAccountingPeriodForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.business.register_next_accounting_period.empty")
      val invalid = ErrorMessageFactory.error("error.business.register_next_accounting_period.invalid")

      empty fieldErrorIs MessageLookup.Error.Business.RegisterNextAccountingPeriod.empty
      empty summaryErrorIs MessageLookup.Error.Business.RegisterNextAccountingPeriod.empty

      invalid fieldErrorIs MessageLookup.Error.Business.RegisterNextAccountingPeriod.invalid
      invalid summaryErrorIs MessageLookup.Error.Business.RegisterNextAccountingPeriod.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = registerNextAccountingPeriodForm.bind(emptyInput0)
      emptyTest0 assert registerNextAccountingPeriod hasExpectedErrors empty

      val emptyInput = DataMap.registerNextAccountingPeriod("")
      val emptyTest = registerNextAccountingPeriodForm.bind(emptyInput)
      emptyTest assert registerNextAccountingPeriod hasExpectedErrors empty

      val invalidInput = DataMap.registerNextAccountingPeriod("α")
      val invalidTest = registerNextAccountingPeriodForm.bind(invalidInput)
      invalidTest assert registerNextAccountingPeriod hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testYes = DataMap.registerNextAccountingPeriod(option_yes)
      registerNextAccountingPeriodForm isValidFor testYes
      val testNo = DataMap.registerNextAccountingPeriod(option_no)
      registerNextAccountingPeriodForm isValidFor testNo
    }
  }

}
