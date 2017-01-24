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
import models.SoleTraderModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class SoleTraderFormSpec extends PlaySpec with OneAppPerTest {

  import SoleTraderForm._

  "The SoleTraderForm" should {
    "transform the request to the form case class" in {
      val testIncomeSource = option_yes
      val testInput = Map(soleTrader -> testIncomeSource)
      val expected = SoleTraderModel(testIncomeSource)
      val actual = soleTraderForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.business.sole_trader.empty")
      val invalid = ErrorMessageFactory.error("error.business.sole_trader.invalid")

      empty fieldErrorIs MessageLookup.Error.Business.SoleTrader.empty
      empty summaryErrorIs MessageLookup.Error.Business.SoleTrader.empty

      invalid fieldErrorIs MessageLookup.Error.Business.SoleTrader.invalid
      invalid summaryErrorIs MessageLookup.Error.Business.SoleTrader.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = soleTraderForm.bind(emptyInput0)
      emptyTest0 assert soleTrader hasExpectedErrors empty

      val emptyInput = DataMap.soleTrader("")
      val emptyTest = soleTraderForm.bind(emptyInput)
      emptyTest assert soleTrader hasExpectedErrors empty

      val invalidInput = DataMap.soleTrader("α")
      val invalidTest = soleTraderForm.bind(invalidInput)
      invalidTest assert soleTrader hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testYes = DataMap.soleTrader(option_yes)
      soleTraderForm isValidFor testYes
      val testNo = DataMap.soleTrader(option_no)
      soleTraderForm isValidFor testNo
    }
  }

}
