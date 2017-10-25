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

package agent.forms

import agent.assets.MessageLookup
import agent.forms.AccountingPeriodPriorForm._
import agent.forms.validation.ErrorMessageFactory
import agent.forms.validation.testutils.{DataMap, _}
import agent.models.AccountingPeriodPriorModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class AccountingPeriodFormSpec extends PlaySpec with OneAppPerTest {

  "The AccountingPeriodForm" should {
    "transform the request to the form case class" in {
      val testIncomeSource = option_yes
      val testInput = Map(accountingPeriodPrior -> testIncomeSource)
      val expected = AccountingPeriodPriorModel(testIncomeSource)
      val actual = accountingPeriodPriorForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.business.current_financial_period_prior.empty")
      val invalid = ErrorMessageFactory.error("error.business.current_financial_period_prior.invalid")

      empty fieldErrorIs MessageLookup.Error.Business.AccountingPeriodPrior.empty
      empty summaryErrorIs MessageLookup.Error.Business.AccountingPeriodPrior.empty

      invalid fieldErrorIs MessageLookup.Error.Business.AccountingPeriodPrior.invalid
      invalid summaryErrorIs MessageLookup.Error.Business.AccountingPeriodPrior.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = accountingPeriodPriorForm.bind(emptyInput0)
      emptyTest0 assert accountingPeriodPrior hasExpectedErrors empty

      val emptyInput = DataMap.accountingPeriodPrior("")
      val emptyTest = accountingPeriodPriorForm.bind(emptyInput)
      emptyTest assert accountingPeriodPrior hasExpectedErrors empty

      val invalidInput = DataMap.accountingPeriodPrior("α")
      val invalidTest = accountingPeriodPriorForm.bind(invalidInput)
      invalidTest assert accountingPeriodPrior hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testYes = DataMap.accountingPeriodPrior(option_yes)
      accountingPeriodPriorForm isValidFor testYes
      val testNo = DataMap.accountingPeriodPrior(option_no)
      accountingPeriodPriorForm isValidFor testNo
    }
  }

}
