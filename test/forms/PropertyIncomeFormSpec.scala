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
import models.PropertyIncomeModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class PropertyIncomeFormSpec extends PlaySpec with OneAppPerTest {

  import PropertyIncomeForm._

  "The PropertyIncomeForm" should {
    "transform the request to the form case class" in {
      val testIncomeValue = option_LT10k
      val testInput = Map(incomeValue -> testIncomeValue)
      val expected = PropertyIncomeModel(testIncomeValue)
      val actual = propertyIncomeForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.property.income.empty")
      val invalid = ErrorMessageFactory.error("error.property.income.invalid")

      empty fieldErrorIs MessageLookup.Error.Property.Income.empty
      empty summaryErrorIs MessageLookup.Error.Property.Income.empty

      invalid fieldErrorIs MessageLookup.Error.Property.Income.invalid
      invalid summaryErrorIs MessageLookup.Error.Property.Income.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = propertyIncomeForm.bind(emptyInput0)
      emptyTest0 assert incomeValue hasExpectedErrors empty

      val emptyInput = DataMap.propertyIncomeValue("")
      val emptyTest = propertyIncomeForm.bind(emptyInput)
      emptyTest assert incomeValue hasExpectedErrors empty

      val invalidInput = DataMap.propertyIncomeValue("α")
      val invalidTest = propertyIncomeForm.bind(invalidInput)
      invalidTest assert incomeValue hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testLT10k = DataMap.propertyIncomeValue(option_LT10k)
      propertyIncomeForm isValidFor testLT10k
      val testGE10k = DataMap.propertyIncomeValue(option_GE10k)
      propertyIncomeForm isValidFor testGE10k
    }
  }

}
