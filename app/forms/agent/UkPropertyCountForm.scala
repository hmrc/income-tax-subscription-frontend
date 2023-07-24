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

package forms.agent

import forms.validation.Constraints.{isNumber, nonEmpty}
import forms.validation.utils.ConstraintUtil.{ConstraintUtil, constraint}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}

object UkPropertyCountForm {

  val fieldName: String = "uk-property-count"

  val emptyErrorKey: String = "error.business.property.count.empty"
  val nonNumericErrorKey: String = "error.business.property.count.numeric"

  val aboveZero: Constraint[Int] = constraint[Int] { number =>
    if (number > 0) {
      Valid
    } else {
      Invalid(emptyErrorKey)
    }
  }

  val form: Form[Int] = Form[Int](
    single(
      fieldName -> default(text, "")
        .verifying(nonEmpty(emptyErrorKey) andThen isNumber(nonNumericErrorKey))
        .transform[Int](_.toInt, _.toString)
        .verifying(aboveZero)
    )
  )

}
