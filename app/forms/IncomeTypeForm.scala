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
import models.IncomeTypeModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object IncomeTypeForm {

  val incomeType = "incomeType"
  val option_cash = "Cash"
  val option_accruals = "Accruals"

  val incomeEmpty: Constraint[String] = constraint[String](
    income => {
      lazy val emptyIncome = ErrorMessageFactory.error("error.income_type.empty")
      if (income.isEmpty) emptyIncome else Valid
    }
  )

  val incomeInvalid: Constraint[String] = constraint[String](
    income => {
      lazy val invalidName = ErrorMessageFactory.error("error.income_type.invalid")
      income match {
        case `option_cash` | `option_accruals` => Valid
        case _ => invalidName
      }
    }
  )

  val incomeTypeForm = Form(
    mapping(
      incomeType -> oText.toText.verifying(incomeEmpty andThen incomeInvalid)
    )(IncomeTypeModel.apply)(IncomeTypeModel.unapply)
  )

}
