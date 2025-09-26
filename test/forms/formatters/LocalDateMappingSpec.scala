/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.data.{Form, FormError}
import utilities.UnitTestTrait

import java.time.LocalDate

class LocalDateMappingSpec extends UnitTestTrait with Matchers with LocalDateMapping {

  val testForm: Form[LocalDate] = Form("field" -> localDate(
    invalidKey = "invalidKey",
    allRequiredKey = "allRequiredKey",
    twoRequiredKey = "twoRequiredKey",
    requiredKey = "requiredKey",
    invalidYearKey = "invalidYearKey")
  )

  "localDate" must {
    "bind a valid date" in {
      val result = testForm.bind(Map(
        "field-dateDay" -> "25",
        "field-dateMonth" -> "10",
        "field-dateYear" -> "2023")
      )
      result.get mustBe LocalDate.of(2023, 10, 25)
    }

    "fail to bind an invalid date" in {
      val result = testForm.bind(Map(
        "field-dateDay" -> "31",
        "field-dateMonth" -> "2",
        "field-dateYear" -> "2023")
      )
      result.errors must contain(FormError("field-dateDay", "invalidKey", Seq()))
    }

    "fail to bind an incomplete date" in {
      val result = testForm.bind(Map(
        "field-dateDay" -> "",
        "field-dateMonth" -> "",
        "field-dateYear" -> "2023")
      )
      result.errors must contain(FormError("field-dateDay", "twoRequiredKey", Seq("day", "month")))
    }

    "fail to bind an empty date" in {
      val result = testForm.bind(Map(
        "field-dateDay" -> "",
        "field-dateMonth" -> "",
        "field-dateYear" -> "")
      )
      result.errors must contain(FormError("field-dateDay", "allRequiredKey", Seq.empty))
    }

    "unbind a valid date" in {
      val result = testForm.fill(LocalDate.of(2023, 10, 25))
      result.data mustBe Map(
        "field-dateDay" -> "25",
        "field-dateMonth" -> "10",
        "field-dateYear" -> "2023"
      )
    }
  }
}
