/*
 * Copyright 2026 HM Revenue & Customs
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

package forms.common

import forms.common.DisplayNameForm.{displayName, displayNameForm, populateNameData, validateDisplayNames}
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError

class DisplayNameFormSpec extends PlaySpec {

  private val emptyError = "error.display-name.business-display-name.empty"
  private val maxLengthError = "error.display-name.business-display-name.max-length"
  private val invalidError = "error.display-name.business-display-name.invalid"
  private val minLengthError = "error.display-name.business-display-name.min-length"

  "displayNameForm" should {
    "bind and trim multiple valid display names" in {
      val boundForm = displayNameForm.bind(Map(
        s"$displayName[0]" -> "  First business  ",
        s"$displayName[1]" -> "Second business"
      ))

      boundForm.errors mustBe empty
      boundForm.value mustBe Some(Seq("First business", "Second business"))
    }

    "return an error when a display name is empty" in {
      val boundForm = displayNameForm.bind(Map(s"$displayName[0]" -> "   "))

      boundForm.value mustBe None
      boundForm.errors mustBe Seq(FormError(s"$displayName[0]", emptyError))
    }

    "return an error when a display name exceeds the maximum length" in {
      val boundForm = displayNameForm.bind(Map(s"$displayName[0]" -> "a" * 36))

      boundForm.value mustBe None
      boundForm.errors mustBe Seq(FormError(s"$displayName[0]", maxLengthError))
    }

    "return an error when a display name contains an invalid character" in {
      val boundForm = displayNameForm.bind(Map(s"$displayName[0]" -> "Business@"))

      boundForm.value mustBe None
      boundForm.errors mustBe Seq(FormError(s"$displayName[0]", invalidError))
    }

    "return an error when a display name has fewer than two letters" in {
      val boundForm = displayNameForm.bind(Map(s"$displayName[0]" -> "123"))

      boundForm.value mustBe None
      boundForm.errors mustBe Seq(FormError(s"$displayName[0]", minLengthError))
    }

    "return errors for duplicate display names" in {
      val boundForm = displayNameForm.bind(Map(
        s"$displayName[0]" -> "Same business",
        s"$displayName[1]" -> "Same business"
      ))

      validateDisplayNames(boundForm, expectedCount = 2).errors.map(_.key) mustBe Seq(
        s"$displayName[0]",
        s"$displayName[1]"
      )
    }

    "return an error when the number of display names does not match the businesses" in {
      val boundForm = displayNameForm.bind(Map(s"$displayName[0]" -> "Business"))

      validateDisplayNames(boundForm, expectedCount = 2).errors mustBe Seq(
        FormError(displayName, "error.display-name.count")
      )
    }

    "populate indexed form data from optional display names" in {
      populateNameData(Seq(Some("First business"), None, Some("Third business"))) mustBe Map(
        s"$displayName[0]" -> "First business",
        s"$displayName[1]" -> "",
        s"$displayName[2]" -> "Third business"
      )
    }
  }
}
