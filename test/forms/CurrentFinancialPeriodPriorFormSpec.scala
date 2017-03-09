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
import forms.CurrentFinancialPeriodPriorForm._
import forms.validation.ErrorMessageFactory
import forms.validation.testutils.{DataMap, _}
import models.CurrentFinancialPeriodPriorModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class CurrentFinancialPeriodPriorFormSpec extends PlaySpec with OneAppPerTest {

  "The CurrentFinancialPeriodPriorForm" should {
    "transform the request to the form case class" in {
      val testIncomeSource = option_yes
      val testInput = Map(currentFinancialPeriodPrior -> testIncomeSource)
      val expected = CurrentFinancialPeriodPriorModel(testIncomeSource)
      val actual = currentFinancialPeriodPriorForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.business.current_financial_period_prior.empty")
      val invalid = ErrorMessageFactory.error("error.business.current_financial_period_prior.invalid")

      empty fieldErrorIs MessageLookup.Error.Business.CurrentFinancialPeriodPrior.empty
      empty summaryErrorIs MessageLookup.Error.Business.CurrentFinancialPeriodPrior.empty

      invalid fieldErrorIs MessageLookup.Error.Business.CurrentFinancialPeriodPrior.invalid
      invalid summaryErrorIs MessageLookup.Error.Business.CurrentFinancialPeriodPrior.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = currentFinancialPeriodPriorForm.bind(emptyInput0)
      emptyTest0 assert currentFinancialPeriodPrior hasExpectedErrors empty

      val emptyInput = DataMap.currentFinancialPeriodPrior("")
      val emptyTest = currentFinancialPeriodPriorForm.bind(emptyInput)
      emptyTest assert currentFinancialPeriodPrior hasExpectedErrors empty

      val invalidInput = DataMap.currentFinancialPeriodPrior("α")
      val invalidTest = currentFinancialPeriodPriorForm.bind(invalidInput)
      invalidTest assert currentFinancialPeriodPrior hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testYes = DataMap.currentFinancialPeriodPrior(option_yes)
      currentFinancialPeriodPriorForm isValidFor testYes
      val testNo = DataMap.currentFinancialPeriodPrior(option_no)
      currentFinancialPeriodPriorForm isValidFor testNo
    }
  }

}
