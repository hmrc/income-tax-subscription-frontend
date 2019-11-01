/*
 * Copyright 2019 HM Revenue & Customs
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
import core.forms.submapping.AccountingYearMapping
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import core.models.Current
import incometax.business.models.AccountingYearModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class AccountingYearFormSpec extends PlaySpec with OneAppPerTest {

  import incometax.business.forms.AccountingYearForm._

  "The AccountingYearForm" should {
    "transform the request to the form case class" in {
      val testAccountingYear = Current
      val testInput = Map(accountingYear -> AccountingYearMapping.option_current)
      val expected = AccountingYearModel(testAccountingYear)
      val actual = accountingYearForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate accounting year correctly" in {
      val empty = ErrorMessageFactory.error("error.what-year.empty")
      val invalid = ErrorMessageFactory.error("error.what-year.invalid")

      empty fieldErrorIs MessageLookup.Error.AccountingYear.empty
      empty summaryErrorIs MessageLookup.Error.AccountingYear.empty

      invalid fieldErrorIs MessageLookup.Error.AccountingYear.invalid
      invalid summaryErrorIs MessageLookup.Error.AccountingYear.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = accountingYearForm.bind(emptyInput0)
      emptyTest0 assert accountingYear hasExpectedErrors empty

      val emptyInput = DataMap.accountingYear("")
      val emptyTest = accountingYearForm.bind(emptyInput)
      emptyTest assert accountingYear hasExpectedErrors empty

      val invalidInput = DataMap.accountingYear("α")
      val invalidTest = accountingYearForm.bind(invalidInput)
      invalidTest assert accountingYear hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testCash = DataMap.accountingYear(AccountingYearMapping.option_current)
      accountingYearForm isValidFor testCash
      val testAccruals = DataMap.accountingYear(AccountingYearMapping.option_next)
      accountingYearForm isValidFor testAccruals
    }
  }

}
