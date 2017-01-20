/*
 * Copyright 2017 HM Revenue & Customs
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

package forms

import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import models.PropertyIncomeModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object PropertyIncomeForm {

  val incomeValue = "incomeValue"
  val option_LT10k = "LT10k" // less than 10k
  val option_GE10k = "GE10k" // 10k or more

  val incomeValueEmpty: Constraint[String] = constraint[String](
    incomeValue => {
      lazy val emptyIncomeValue = ErrorMessageFactory.error("error.property.income.empty")
      if (incomeValue.isEmpty) emptyIncomeValue else Valid
    }
  )

  val incomeValueInvalid: Constraint[String] = constraint[String](
    incomeValue => {
      lazy val invalidIncomeValue = ErrorMessageFactory.error("error.property.income.invalid")
      incomeValue match {
        case `option_LT10k` | `option_GE10k` => Valid
        case _ => invalidIncomeValue
      }
    }
  )

  val propertyIncomeForm = Form(
    mapping(
      incomeValue -> oText.toText.verifying(incomeValueEmpty andThen incomeValueInvalid)
    )(PropertyIncomeModel.apply)(PropertyIncomeModel.unapply)
  )

}
