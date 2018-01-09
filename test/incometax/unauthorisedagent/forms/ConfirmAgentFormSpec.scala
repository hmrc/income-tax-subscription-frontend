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

package incometax.unauthorisedagent.forms

import assets.MessageLookup
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import incometax.unauthorisedagent.forms.ConfirmAgentForm._
import incometax.unauthorisedagent.models.ConfirmAgentModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class ConfirmAgentFormSpec extends PlaySpec with OneAppPerTest {

  "The ConfirmAgentForm" should {
    "transform the request to the form case class" in {
      val testChoice = option_no
      val testInput = Map(choice -> testChoice)
      val expected = ConfirmAgentModel(testChoice)
      val actual = confirmAgentForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.confirm-agent.empty")
      val invalid = ErrorMessageFactory.error("error.confirm-agent.invalid")

      empty fieldErrorIs MessageLookup.Error.UnauthroisedAgent.ConfirmAgent.empty
      empty summaryErrorIs MessageLookup.Error.UnauthroisedAgent.ConfirmAgent.empty

      invalid fieldErrorIs MessageLookup.Error.UnauthroisedAgent.ConfirmAgent.invalid
      invalid summaryErrorIs MessageLookup.Error.UnauthroisedAgent.ConfirmAgent.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = confirmAgentForm.bind(emptyInput0)
      emptyTest0 assert choice hasExpectedErrors empty

      val emptyInput = DataMap.confirmAgent("")
      val emptyTest = confirmAgentForm.bind(emptyInput)
      emptyTest assert choice hasExpectedErrors empty

      val invalidInput = DataMap.confirmAgent("α")
      val invalidTest = confirmAgentForm.bind(invalidInput)
      invalidTest assert choice hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testsYes = DataMap.confirmAgent(option_yes)
      confirmAgentForm isValidFor testsYes
      val testsNo = DataMap.confirmAgent(option_no)
      confirmAgentForm isValidFor testsNo
    }

  }
}
