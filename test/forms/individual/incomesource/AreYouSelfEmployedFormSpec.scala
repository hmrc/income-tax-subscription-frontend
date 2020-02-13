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

package forms.individual.incomesource

import assets.MessageLookup
import forms.submapping.YesNoMapping
import forms.validation.ErrorMessageFactory
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.No
import models.individual.incomesource.AreYouSelfEmployedModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class AreYouSelfEmployedFormSpec extends PlaySpec with OneAppPerTest {

  import forms.individual.incomesource.AreYouSelfEmployedForm._

  "The AreYouSelfEmployedForm" should {
    "transform the request to the form case class" in {
      val testChoice = No
      val testInput = Map(choice -> YesNoMapping.option_no)
      val expected = AreYouSelfEmployedModel(testChoice)
      val actual = areYouSelfEmployedForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.are_you_selfemployed.empty")
      val invalid = ErrorMessageFactory.error("error.are_you_selfemployed.invalid")

      empty fieldErrorIs MessageLookup.Error.AreYouSelfEmployed.empty
      empty summaryErrorIs MessageLookup.Error.AreYouSelfEmployed.empty

      invalid fieldErrorIs MessageLookup.Error.AreYouSelfEmployed.invalid
      invalid summaryErrorIs MessageLookup.Error.AreYouSelfEmployed.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = areYouSelfEmployedForm.bind(emptyInput0)
      emptyTest0 assert choice hasExpectedErrors empty

      val emptyInput = DataMap.areYouSelfEmployed("")
      val emptyTest = areYouSelfEmployedForm.bind(emptyInput)
      emptyTest assert choice hasExpectedErrors empty

      val invalidInput = DataMap.areYouSelfEmployed("α")
      val invalidTest = areYouSelfEmployedForm.bind(invalidInput)
      invalidTest assert choice hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testsYes = DataMap.areYouSelfEmployed(YesNoMapping.option_yes)
      areYouSelfEmployedForm isValidFor testsYes
      val testsNo = DataMap.areYouSelfEmployed(YesNoMapping.option_no)
      areYouSelfEmployedForm isValidFor testsNo
    }

  }
}
