/*
 * Copyright 2019 HM Revenue & Customs
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

package core.forms.validation

import core.forms.validation.models.{FieldError, SummaryError}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import org.scalatest.Matchers._

class ErrorMessageFactorySpec extends PlaySpec with OneServerPerSuite {

  "ErrorMessageFactory" should {
    "correctly package the Invalid instance when both the field and summary errors are explicitly defined " in {
      val fieldError = FieldError("errMsgField", Seq("arg1", "arg2"))
      val summaryError = SummaryError("errMsgSummary", Seq("arg1", "arg2"))

      val actual = ErrorMessageFactory.error(fieldError, summaryError)
      actual.errors.head.message shouldBe ""
      actual.errors.head.args.head shouldBe fieldError
      actual.errors.head.args(1) shouldBe summaryError
    }

    "correctly package the Invalid instance when the shortcut function is used" in {
      val errMsg = "errMsg"
      val errArgs = Seq("arg1", "arg2")
      val actual = ErrorMessageFactory.error(errMsg, errArgs: _*)
      actual.errors.head.message shouldBe ""
      actual.errors.head.args.head shouldBe FieldError(errMsg, errArgs)
      actual.errors.head.args(1) shouldBe SummaryError(errMsg, errArgs)
    }
  }

}
