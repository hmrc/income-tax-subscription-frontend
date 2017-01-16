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

import forms.validation.ErrorMessageFactory
import forms.validation.testutils.{DataMap, _}
import models.IncomeTypeModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class IncomeTypeFormSpec extends PlaySpec with OneAppPerTest {

  import IncomeTypeForm._

  "The IncomeTypeForm" should {
    "transform the request to the form case class" in {
      val testIncomeType = option_cash
      val testInput = Map(incomeType -> testIncomeType)
      val expected = IncomeTypeModel(testIncomeType)
      val actual = incomeTypeForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.income_type.empty")
      val invalid = ErrorMessageFactory.error("error.income_type.invalid")

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = incomeTypeForm.bind(emptyInput0)
      emptyTest0 assert incomeType hasExpectedErrors empty

      val emptyInput = DataMap.inType("")
      val emptyTest = incomeTypeForm.bind(emptyInput)
      emptyTest assert incomeType hasExpectedErrors empty

      val invalidInput = DataMap.inType("α")
      val invalidTest = incomeTypeForm.bind(invalidInput)
      invalidTest assert incomeType hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testCash = DataMap.inType(option_cash)
      incomeTypeForm isValidFor testCash
      val testAccruals = DataMap.inType(option_accruals)
      incomeTypeForm isValidFor testAccruals
    }
  }

}
