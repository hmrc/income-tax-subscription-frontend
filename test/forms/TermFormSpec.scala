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
import models.TermModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class TermFormSpec extends PlaySpec with OneAppPerTest {

  import TermForm._

  "The termForm " should {
    "transform the data to the case class" in {
      val testTerm = true
      val testInput = Map(hasAcceptedTerms -> testTerm.toString)
      val expected = TermModel(testTerm)
      val actual = termForm.bind(testInput).value
      actual shouldBe Some(expected)
    }

    "validate terms correctly" in {
      val empty = ErrorMessageFactory.error("error.terms.empty")

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = termForm.bind(emptyInput0)
      emptyTest0 assert hasAcceptedTerms hasExpectedErrors empty

      val emptyInput = DataMap.terms("")
      val emptyTest = termForm.bind(emptyInput)
      emptyTest assert hasAcceptedTerms hasExpectedErrors empty

      val invalidInput = DataMap.terms(false)
      val invalidTest = termForm.bind(invalidInput)
      invalidTest assert hasAcceptedTerms hasExpectedErrors empty
    }

    "The following submissions should be valid" in {
      val valid = DataMap.terms(true)
      termForm isValidFor valid
    }
  }
}
