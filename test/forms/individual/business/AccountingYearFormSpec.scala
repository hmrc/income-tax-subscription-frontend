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

import forms.individual.business.AccountingYearForm._
import forms.submapping.AccountingYearMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.Current
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.FormError

class AccountingYearFormSpec extends PlaySpec with GuiceOneAppPerTest {

  "The AccountingYearForm" should {
    "transform the request to the form case class" in {
      val testInput = Map(accountingYear -> AccountingYearMapping.option_current)
      val expected = Current
      val actual = accountingYearForm.bind(testInput).value

      actual mustBe Some(expected)
    }

    "validate accounting year correctly" when {
      val empty = "error.what-year.empty"
      val invalid = "error.what-year.invalid"

      "the map be empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = accountingYearForm.bind(emptyInput0)
        emptyTest0.errors must contain(FormError(accountingYear,empty))
      }
      "the name be empty" in {
        val emptyInput = DataMap.accountingYear("")
        val emptyTest = accountingYearForm.bind(emptyInput)
        emptyTest.errors must contain(FormError(accountingYear,empty))
      }
      "the input should be invalid" in {
        val invalidInput = DataMap.accountingYear("α")
        val invalidTest = accountingYearForm.bind(invalidInput)
        invalidTest.errors must contain(FormError(accountingYear,invalid))
      }
    }

      "The Cash submission should be valid" in {
        val testCash = DataMap.accountingYear(AccountingYearMapping.option_current)
        accountingYearForm isValidFor testCash
      }

      "The Accruals submission should be valid" in {
        val testAccruals = DataMap.accountingYear(AccountingYearMapping.option_current)
        accountingYearForm isValidFor testAccruals
      }

      "The Accruals and next year submission should be valid" in {
        val testAccruals = DataMap.accountingYear(AccountingYearMapping.option_next)
        accountingYearForm isValidFor testAccruals
      }
    }

  }
