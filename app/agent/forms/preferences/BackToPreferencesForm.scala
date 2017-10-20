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

package agent.forms.preferences

import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil.constraint
import forms.validation.utils.MappingUtil._
import models.preferences.BackToPreferencesModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid}


object BackToPreferencesForm {

  val backToPreferences = "backToPreferences"
  val option_yes = "Yes"
  val option_no = "No"

  val backToPreferencesEmpty: Constraint[String] = constraint[String](
    terms => {
      lazy val empty = ErrorMessageFactory.error("error.back_to_preferences.empty")
      terms match {
        case `option_yes` | `option_no` => Valid
        case _ => empty
      }
    }
  )

  val backToPreferencesForm = Form(
    mapping(
      backToPreferences -> oText.toText.verifying(backToPreferencesEmpty)
    )(BackToPreferencesModel.apply)(BackToPreferencesModel.unapply)
  )

}
