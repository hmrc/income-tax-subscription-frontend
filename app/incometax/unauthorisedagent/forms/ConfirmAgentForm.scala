/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.unauthorisedagent.forms

import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil._
import core.forms.validation.utils.MappingUtil._
import incometax.unauthorisedagent.models.ConfirmAgentModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid}

object ConfirmAgentForm {

  val choice = "choice"
  val option_yes = "Yes"
  val option_no = "No"

  val choiceEmpty: Constraint[String] = constraint[String](
    choice => {
      lazy val emptyChoice = ErrorMessageFactory.error("error.confirm-agent.empty")
      if (choice.isEmpty) emptyChoice else Valid
    }
  )

  val choiceInvalid: Constraint[String] = constraint[String](
    choice => {
      lazy val invalidChoice = ErrorMessageFactory.error("error.confirm-agent.invalid")
      choice match {
        case `option_yes` | `option_no` => Valid
        case _ => invalidChoice
      }
    }
  )

  val confirmAgentForm = Form(
    mapping(
      choice -> oText.toText.verifying(choiceEmpty andThen choiceInvalid)
    )(ConfirmAgentModel.apply)(ConfirmAgentModel.unapply)
  )

}
