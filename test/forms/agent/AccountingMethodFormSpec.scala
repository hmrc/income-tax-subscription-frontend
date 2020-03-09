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

package forms.agent

import forms.submapping.AccountingMethodMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.Cash
import models.agent.AccountingMethodModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.data.FormError

class AccountingMethodFormSpec extends PlaySpec with OneAppPerTest {

  import forms.agent.AccountingMethodForm._

  "The AccountingMethodForm" should {
    "transform the request to the form case class" in {
      val testAccountingMethod = Cash
      val testInput = Map(accountingMethod -> AccountingMethodMapping.option_cash)
      val expected = AccountingMethodModel(testAccountingMethod)
      val actual = accountingMethodForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" should {
      val empty = "agent.error.accounting-method.empty"
      val invalid = "agent.error.accounting-method.invalid"

      "show an empty error when the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = accountingMethodForm.bind(emptyInput0)
        emptyTest0.errors should contain(FormError(accountingMethod, empty))
      }

      "show an empty error when the input is empty" in {
        val emptyInput = DataMap.accountingMethod("")
        val emptyTest = accountingMethodForm.bind(emptyInput)
        emptyTest.errors should contain(FormError(accountingMethod, empty))
      }

      "show invalid when the input is invalid" in {
        val invalidInput = DataMap.accountingMethod("α")
        val invalidTest = accountingMethodForm.bind(invalidInput)
        invalidTest.errors should contain(FormError(accountingMethod, invalid))
      }

      "The following submission should be valid" in {
        val testCash = DataMap.accountingMethod(AccountingMethodMapping.option_cash)
        accountingMethodForm isValidFor testCash
      }

      "The Accruals submission should be valid" in {
        val testAccruals = DataMap.accountingMethod(AccountingMethodMapping.option_accruals)
        accountingMethodForm isValidFor testAccruals
      }
    }

  }
}

