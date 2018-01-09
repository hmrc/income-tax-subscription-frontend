/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.forms

import agent.assets.MessageLookup
import agent.forms.validation.testutils.DataMap
import agent.models.NotEligibleModel
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap => _, _}
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class NotEligibleFormSpec extends PlaySpec with OneAppPerTest {

  import NotEligibleForm._

  "The NotEligibleForm" should {
    "transform the request to the form case class" in {
      val testChoice = option_signout
      val testInput = Map(choice -> testChoice)
      val expected = NotEligibleModel(testChoice)
      val actual = notEligibleForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("agent.error.not-eligible.empty")
      val invalid = ErrorMessageFactory.error("agent.error.not-eligible.invalid")

      empty fieldErrorIs MessageLookup.Error.NotEligible.empty
      empty summaryErrorIs MessageLookup.Error.NotEligible.empty

      invalid fieldErrorIs MessageLookup.Error.NotEligible.invalid
      invalid summaryErrorIs MessageLookup.Error.NotEligible.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = notEligibleForm.bind(emptyInput0)
      emptyTest0 assert choice hasExpectedErrors empty

      val emptyInput = DataMap.notEligibleChoice("")
      val emptyTest = notEligibleForm.bind(emptyInput)
      emptyTest assert choice hasExpectedErrors empty

      val invalidInput = DataMap.notEligibleChoice("α")
      val invalidTest = notEligibleForm.bind(invalidInput)
      invalidTest assert choice hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testSignOut = DataMap.notEligibleChoice(option_signout)
      notEligibleForm isValidFor testSignOut
      val testSignUp = DataMap.notEligibleChoice(option_signup)
      notEligibleForm isValidFor testSignUp
    }
  }

}
