/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.formatters

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import play.api.data.format.Formatter

class FormattersSpec extends AnyWordSpecLike with Matchers with Formatters {

  def stringFormatter: Formatter[String] = stringFormatter("error.required")

  val bindKey: String = "testKey"

  "stringFormatter.bind" when {
    "bind" must {
      "return a string when a non empty value is provided" in {
        val result = stringFormatter.bind(bindKey, Map(bindKey -> "testValue"))
        result mustBe Right("testValue")
      }
      "return a form error" when {
        "no value is provided" in {
          val result = stringFormatter.bind(bindKey, Map.empty)
          result mustBe Left(Seq(FormError(bindKey, "error.required", Seq.empty)))
        }
        "only whitespace is provided" in {
          val result = stringFormatter.bind(bindKey, Map(bindKey -> "   "))
          result mustBe Left(Seq(FormError(bindKey, "error.required", Seq.empty)))
        }
      }
    }
    "unbind" must {
      "return the correct map of key and value" in {
        val result = stringFormatter.unbind(bindKey, "testValue")
        result mustBe Map(bindKey -> "testValue")
      }
    }
  }

}
