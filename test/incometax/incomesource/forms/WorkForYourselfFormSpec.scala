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

package incometax.incomesource.forms

import assets.MessageLookup
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import incometax.incomesource.models.WorkForYourselfModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class WorkForYourselfFormSpec extends PlaySpec with OneAppPerTest {

  import WorkForYourselfForm._

  "The WorkForYourselfForm" should {
    "transform the request to the form case class" in {
      val testChoice = option_no
      val testInput = Map(choice -> testChoice)
      val expected = WorkForYourselfModel(testChoice)
      val actual = workForYourselfForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" in {
      val empty = ErrorMessageFactory.error("error.work_for_yourself.empty")
      val invalid = ErrorMessageFactory.error("error.work_for_yourself.invalid")

      empty fieldErrorIs MessageLookup.Error.WorkForYourself.empty
      empty summaryErrorIs MessageLookup.Error.WorkForYourself.empty

      invalid fieldErrorIs MessageLookup.Error.WorkForYourself.invalid
      invalid summaryErrorIs MessageLookup.Error.WorkForYourself.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = workForYourselfForm.bind(emptyInput0)
      emptyTest0 assert choice hasExpectedErrors empty

      val emptyInput = DataMap.workForYourself("")
      val emptyTest = workForYourselfForm.bind(emptyInput)
      emptyTest assert choice hasExpectedErrors empty

      val invalidInput = DataMap.workForYourself("α")
      val invalidTest = workForYourselfForm.bind(invalidInput)
      invalidTest assert choice hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testsYes = DataMap.workForYourself(option_yes)
      workForYourselfForm isValidFor testsYes
      val testsNo = DataMap.workForYourself(option_no)
      workForYourselfForm isValidFor testsNo
    }

  }
}
