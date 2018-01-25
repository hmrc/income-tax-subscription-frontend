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

import core.forms.validation.ErrorMessageFactory
import core.forms.validation.models.TargetIds
import core.forms.validation.utils.ConstraintUtil._
import core.forms.validation.utils.MappingUtil._
import incometax.incomesource.models.RentUkPropertyModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid}

object RentUkPropertyForm {

  val rentUkProperty = "rentUkProperty"
  val onlySourceOfSelfEmployedIncome = "onlySourceOfSelfEmployedIncome"
  val option_yes = "Yes"
  val option_no = "No"

  val choiceEmpty: Constraint[String] = constraint[String](
    choice => {
      lazy val emptyChoice = ErrorMessageFactory.error("error.rent-uk-property.empty")
      if (choice.isEmpty) emptyChoice else Valid
    }
  )

  val choiceInvalid: Constraint[String] = constraint[String](
    choice => {
      lazy val invalidChoice = ErrorMessageFactory.error("error.rent-uk-property.invalid")
      choice match {
        case `option_yes` | `option_no` => Valid
        case _ => invalidChoice
      }
    }
  )

  val crossFieldEmptyRentUkProperty: Constraint[RentUkPropertyModel] = constraint[RentUkPropertyModel](
    rentUkPropertyModel => {
      lazy val empty = ErrorMessageFactory.error(TargetIds(onlySourceOfSelfEmployedIncome), "error.rent-uk-property.only-source-empty")
      if (rentUkPropertyModel.rentUkProperty == option_yes &&
        rentUkPropertyModel.onlySourceOfSelfEmployedIncome.isEmpty) empty else Valid
    }
  )

  val crossFieldInvalidRentUkProperty: Constraint[RentUkPropertyModel] = constraint[RentUkPropertyModel](
    rentUkPropertyModel => {
      lazy val invalid = ErrorMessageFactory.error(TargetIds(onlySourceOfSelfEmployedIncome), "error.rent-uk-property.only-source-invalid")

      if (rentUkPropertyModel.rentUkProperty == option_yes) {
        rentUkPropertyModel.onlySourceOfSelfEmployedIncome match {
          case Some(`option_yes`) | Some(`option_no`) => Valid
          case _ => invalid
        }
      } else Valid
    }
  )

  val rentUkPropertyForm = Form(
    mapping(
      rentUkProperty -> oText.toText.verifying(choiceEmpty andThen choiceInvalid),
      onlySourceOfSelfEmployedIncome -> oText
    )(RentUkPropertyModel.apply)(RentUkPropertyModel.unapply).verifying(crossFieldEmptyRentUkProperty andThen crossFieldInvalidRentUkProperty)
  )
}
