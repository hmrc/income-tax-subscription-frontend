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
import models.SoleTraderModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object SoleTraderForm {

  val soleTrader = "soleTrader"
  val option_yes = "Yes"
  val option_no = "No"

  val soleTraderEmpty: Constraint[String] = constraint[String](
    source => {
      lazy val emptySource = ErrorMessageFactory.error("error.business.sole_trader.empty")
      if (source.isEmpty) emptySource else Valid
    }
  )

  val soleTraderInvalid: Constraint[String] = constraint[String](
    source => {
      lazy val invalidSource = ErrorMessageFactory.error("error.business.sole_trader.invalid")
      source match {
        case `option_yes` | `option_no` => Valid
        case _ => invalidSource
      }
    }
  )

  val soleTraderForm = Form(
    mapping(
      soleTrader -> oText.toText.verifying(soleTraderEmpty andThen soleTraderInvalid)
    )(SoleTraderModel.apply)(SoleTraderModel.unapply)
  )

}
