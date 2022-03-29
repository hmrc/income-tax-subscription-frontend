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

package forms.agent

import forms.submapping.AccountingMethodMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.Cash
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.FormError

class AccountingMethodOverseasFormSpec extends PlaySpec with GuiceOneAppPerTest {

  import forms.agent.AccountingMethodOverseasPropertyForm._

  "The AccountingMethodOverseasForm" should {
    "transform the request to the form case class" in {
      val testInput = Map(accountingMethodOverseasProperty -> AccountingMethodMapping.option_cash)
      val actual = accountingMethodOverseasPropertyForm.bind(testInput).value

      actual mustBe Some(Cash)
    }

    "validate income type correctly" should {
      val empty = "error.agent.overseas_property_accounting_method.empty"
      val invalid = "error.agent.overseas_property_accounting_method.empty"

      "show an empty error when the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = accountingMethodOverseasPropertyForm.bind(emptyInput0)
        emptyTest0.errors must contain(FormError(accountingMethodOverseasProperty, empty))
      }

      "show an empty error when the input is empty" in {
        val emptyInput = DataMap.overseasPropertyAccountingMethod("")
        val emptyTest = accountingMethodOverseasPropertyForm.bind(emptyInput)
        emptyTest.errors must contain(FormError(accountingMethodOverseasProperty, empty))
      }

      "show invalid when the input is invalid" in {
        val invalidInput = DataMap.overseasPropertyAccountingMethod("α")
        val invalidTest = accountingMethodOverseasPropertyForm.bind(invalidInput)
        invalidTest.errors must contain(FormError(accountingMethodOverseasProperty, invalid))
      }

      "The following submission should be valid" in {
        val testCash = DataMap.overseasPropertyAccountingMethod(AccountingMethodMapping.option_cash)
        accountingMethodOverseasPropertyForm isValidFor testCash
      }

      "The Accruals submission should be valid" in {
        val testAccruals = DataMap.overseasPropertyAccountingMethod(AccountingMethodMapping.option_accruals)
        accountingMethodOverseasPropertyForm isValidFor testAccruals
      }
    }

  }
}
