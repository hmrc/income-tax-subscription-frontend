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

package forms.individual.business

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError

class UkPropertyCountFormSpec extends PlaySpec {

  "form" must {
    "bind successfully" when {
      "a number of properties greater than 0 is provided to the form" in {
        val inputNumber: Int = 1
        val boundForm = UkPropertyCountForm.form.bind(Map(UkPropertyCountForm.fieldName -> inputNumber.toString))
        boundForm.value mustBe Some(inputNumber)
      }
    }

    "fail to bind" when {
      "no value is provided to the form" in {
        val boundForm = UkPropertyCountForm.form.bind(Map[String, String]())
        boundForm.errors mustBe Seq(FormError(UkPropertyCountForm.fieldName, UkPropertyCountForm.emptyErrorKey))
      }
      "an empty value is provided to the form" in {
        val boundForm = UkPropertyCountForm.form.bind(Map(UkPropertyCountForm.fieldName -> ""))
        boundForm.errors mustBe Seq(FormError(UkPropertyCountForm.fieldName, UkPropertyCountForm.emptyErrorKey))
      }
      "0 is provided as the number of properties to the form" in {
        val boundForm = UkPropertyCountForm.form.bind(Map(UkPropertyCountForm.fieldName -> "0"))
        boundForm.errors mustBe Seq(FormError(UkPropertyCountForm.fieldName, UkPropertyCountForm.emptyErrorKey))
      }
      "a negative number is provided as the properties to the form" in {
        val boundForm = UkPropertyCountForm.form.bind(Map(UkPropertyCountForm.fieldName -> "-1"))
        boundForm.errors mustBe Seq(FormError(UkPropertyCountForm.fieldName, UkPropertyCountForm.emptyErrorKey))
      }
      "a non numeric value is provided to the form" in {
        val boundForm = UkPropertyCountForm.form.bind(Map(UkPropertyCountForm.fieldName -> "one"))
        boundForm.errors mustBe Seq(FormError(UkPropertyCountForm.fieldName, UkPropertyCountForm.nonNumericErrorKey))
      }
    }
  }

}
