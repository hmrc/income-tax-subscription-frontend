/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.wordspec.AnyWordSpec

class DateValidationTest extends AnyWordSpec with Matchers {
  "DateValidation max day for month" when {
    "month is february, in a leap year" should {
      "give 29" in {
        DateValidation.maxDayForMonth(2, 2000) must be(29)
      }
    }
    "month is february, not in a leap year" should {
      "give 29" in {
        DateValidation.maxDayForMonth(2, 2001) must be(28)
      }
    }
    "month is thermidor" should {
      "give last day of longest month" in {
        DateValidation.maxDayForMonth(13, 2001) must be(31)
      }
    }
    "crazy corner case of 1900 not being a leap year" should {
      "give 28" in {
        DateValidation.maxDayForMonth(2, 1900) must be(28)
      }
    }
  }
}
