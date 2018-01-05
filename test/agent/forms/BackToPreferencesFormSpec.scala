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
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap => _, _}
import agent.forms.validation.testutils.DataMap
import agent.models.preferences.BackToPreferencesModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class BackToPreferencesFormSpec extends PlaySpec with OneAppPerTest {

  import agent.forms.preferences.BackToPreferencesForm._

  "The BackToPreferencesForm " should {
    "transform the data to the case class" in {
      val testTerm = option_yes
      val testInput = Map(backToPreferences -> testTerm.toString)
      val expected = BackToPreferencesModel(testTerm)
      val actual = backToPreferencesForm.bind(testInput).value
      actual shouldBe Some(expected)
    }

    "validate backToPreferences correctly" in {
      val empty = ErrorMessageFactory.error("agent.error.back_to_preferences.empty")

      empty fieldErrorIs MessageLookup.Error.BackToPreferences.empty
      empty summaryErrorIs MessageLookup.Error.BackToPreferences.empty

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = backToPreferencesForm.bind(emptyInput0)
      emptyTest0 assert backToPreferences hasExpectedErrors empty

      val emptyInput = DataMap.acceptPaperlessDataMap("")
      val emptyTest = backToPreferencesForm.bind(emptyInput)
      emptyTest assert backToPreferences hasExpectedErrors empty
    }

    "The following submissions should be valid" in {
      val validYes = DataMap.acceptPaperlessDataMap(option_yes)
      backToPreferencesForm isValidFor validYes

      val validNo = DataMap.acceptPaperlessDataMap(option_no)
      backToPreferencesForm isValidFor validNo
    }
  }
}
