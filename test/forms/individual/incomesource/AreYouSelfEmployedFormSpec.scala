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

import forms.submapping.YesNoMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.No
import models.individual.incomesource.AreYouSelfEmployedModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.data.FormError

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

    "validate income type correctly" should {
      val empty = "error.are_you_selfemployed.empty"
      val invalid = "error.are_you_selfemployed.invalid"


      "show an empty error when the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = areYouSelfEmployedForm.bind(emptyInput0)
        emptyTest0.errors should contain(FormError(choice, empty))
      }
      "show an empty error when the input is empty" in {
        val emptyInput = DataMap.areYouSelfEmployed("")
        val emptyTest = areYouSelfEmployedForm.bind(emptyInput)
        emptyTest.errors should contain(FormError(choice, empty))
      }
      "show invalid when the input is invalid" in {
        val invalidInput = DataMap.areYouSelfEmployed("α")
        val invalidTest = areYouSelfEmployedForm.bind(invalidInput)
        invalidTest.errors should contain(FormError(choice, invalid))
      }
    }

    "The yes submission should be valid" in {
      val testsYes = DataMap.areYouSelfEmployed(YesNoMapping.option_yes)
      areYouSelfEmployedForm isValidFor testsYes
    }
    "The no submission should be valid" in {
      val testsNo = DataMap.areYouSelfEmployed(YesNoMapping.option_no)
      areYouSelfEmployedForm isValidFor testsNo
    }

  }
}
