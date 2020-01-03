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

package incometax.business.forms

import assets.MessageLookup
import core.forms.submapping.AccountingMethodMapping
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import core.models.Cash
import incometax.business.models.AccountingMethodModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class AccountingMethodFormSpec extends PlaySpec with OneAppPerTest {

  import incometax.business.forms.AccountingMethodForm._

  "The AccountingMethodForm" should {
    "transform the request to the form case class" in {
      val testAccountingMethod = Cash
      val testInput = Map(accountingMethod -> AccountingMethodMapping.option_cash)
      val expected = AccountingMethodModel(testAccountingMethod)
      val actual = accountingMethodForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.accounting-method.empty")
      val invalid = ErrorMessageFactory.error("error.accounting-method.invalid")

      empty fieldErrorIs MessageLookup.Error.AccountingMethod.empty
      empty summaryErrorIs MessageLookup.Error.AccountingMethod.empty

      invalid fieldErrorIs MessageLookup.Error.AccountingMethod.invalid
      invalid summaryErrorIs MessageLookup.Error.AccountingMethod.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = accountingMethodForm.bind(emptyInput0)
      emptyTest0 assert accountingMethod hasExpectedErrors empty

      val emptyInput = DataMap.accountingMethod("")
      val emptyTest = accountingMethodForm.bind(emptyInput)
      emptyTest assert accountingMethod hasExpectedErrors empty

      val invalidInput = DataMap.accountingMethod("α")
      val invalidTest = accountingMethodForm.bind(invalidInput)
      invalidTest assert accountingMethod hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testCash = DataMap.accountingMethod(AccountingMethodMapping.option_cash)
      accountingMethodForm isValidFor testCash
      val testAccruals = DataMap.accountingMethod(AccountingMethodMapping.option_accruals)
      accountingMethodForm isValidFor testAccruals
    }
  }

}
