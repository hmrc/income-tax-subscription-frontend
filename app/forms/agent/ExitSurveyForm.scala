/*
 * Copyright 2020 HM Revenue & Customs
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

import agent.models.ExitSurveyModel
import forms.prevalidation.PreprocessedForm
import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil.constraint
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Valid}

object ExitSurveyForm {

  val satisfaction = "satisfaction"
  val improvements = "improvements"
  val improvementsMaxLength = 1200

  val nameTooLong: Constraint[Option[String]] = constraint[Option[String]] {
    case Some(name) if name.trim.length > improvementsMaxLength =>
      ErrorMessageFactory.error("agent.error.survey-feedback.maxLength")
    case _ => Valid
  }

  val exitSurveyValidationForm = Form(
    mapping(
      satisfaction -> optional(text),
      improvements -> optional(text).verifying(nameTooLong)
    )(ExitSurveyModel.apply)(ExitSurveyModel.unapply)
  )

  val exitSurveyForm = PreprocessedForm(exitSurveyValidationForm)

}
