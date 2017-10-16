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
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import models.IncomeSourceModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class IncomeSourceFormSpec extends PlaySpec with OneAppPerTest {

  import IncomeSourceForm._

  "The IncomeSourceForm" should {
    "transform the request to the form case class" in {
      val testIncomeSource = option_business
      val testInput = Map(incomeSource -> testIncomeSource)
      val expected = IncomeSourceModel(testIncomeSource)
      val actual = incomeSourceForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.income_source.empty")
      val invalid = ErrorMessageFactory.error("error.income_source.invalid")

      empty fieldErrorIs MessageLookup.Error.IncomeSource.empty
      empty summaryErrorIs MessageLookup.Error.IncomeSource.empty

      invalid fieldErrorIs MessageLookup.Error.IncomeSource.invalid
      invalid summaryErrorIs MessageLookup.Error.IncomeSource.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = incomeSourceForm.bind(emptyInput0)
      emptyTest0 assert incomeSource hasExpectedErrors empty

      val emptyInput = DataMap.incomeSource("")
      val emptyTest = incomeSourceForm.bind(emptyInput)
      emptyTest assert incomeSource hasExpectedErrors empty

      val invalidInput = DataMap.incomeSource("α")
      val invalidTest = incomeSourceForm.bind(invalidInput)
      invalidTest assert incomeSource hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testBusiness = DataMap.incomeSource(option_business)
      incomeSourceForm isValidFor testBusiness
      val testProperty = DataMap.incomeSource(option_property)
      incomeSourceForm isValidFor testProperty
      val testBoth = DataMap.incomeSource(option_both)
      incomeSourceForm isValidFor testBoth
    }
  }

}
