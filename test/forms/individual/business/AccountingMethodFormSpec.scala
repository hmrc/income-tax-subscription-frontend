/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.individual.business

import forms.submapping.AccountingMethodMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.Cash
import models.common.business.AccountingMethodModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.FormError

class AccountingMethodFormSpec extends PlaySpec with GuiceOneAppPerTest {

  import forms.individual.business.AccountingMethodForm._

  "The AccountingMethodForm" should {
    "transform the request to the form case class" in {
      val testAccountingMethod = Cash
      val testInput = Map(accountingMethod -> AccountingMethodMapping.option_cash)
      val expected = AccountingMethodModel(testAccountingMethod)
      val actual = accountingMethodForm.bind(testInput).value

      actual mustBe Some(expected)
    }
  }

    "validate income type correctly" should {
      val empty = "error.accounting-method.empty"
      val invalid = "error.accounting-method.invalid"

        "show an empty error when the map is empty" in {
          val emptyInput0 = DataMap.EmptyMap
          val emptyTest0 = accountingMethodForm.bind(emptyInput0)
          emptyTest0.errors must contain(FormError(accountingMethod, empty))
        }
        "show an empty error when the input is empty" in {
          val emptyInput = DataMap.accountingMethod("")
          val emptyTest = accountingMethodForm.bind(emptyInput)
          emptyTest.errors must contain(FormError(accountingMethod, empty))
        }
        "show invalid when the input is invalid" in {
          val invalidInput = DataMap.accountingMethod("α")
          val invalidTest = accountingMethodForm.bind(invalidInput)
          invalidTest.errors must contain(FormError(accountingMethod, invalid))
        }

      "The Cash submission should be valid" in {
        val testCash = DataMap.accountingMethod(AccountingMethodMapping.option_cash)
        accountingMethodForm isValidFor testCash
      }
      "The Accruals submission should be valid" in {
        val testAccruals = DataMap.accountingMethod(AccountingMethodMapping.option_accruals)
        accountingMethodForm isValidFor testAccruals
      }
    }
  }

