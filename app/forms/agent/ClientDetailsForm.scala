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

package forms.agent

import forms.prevalidation.PreprocessedForm
import forms.submapping.DateMapping.dateMapping
import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import forms.validation.utils.Patterns
import models.{ClientDetailsModel, DateModel}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object ClientDetailsForm {

  val clientFirstName = "clientFirstName"
  val clientLastName = "clientLastName"
  val clientNino = "clientNino"
  val clientDateOfBirth = "clientDateOfBirth"
  val ninoRegex = """^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$"""

  val nameMaxLength = 105

  val nameEmpty: Constraint[String] = constraint[String](
    name => {
      lazy val emptyName = ErrorMessageFactory.error("error.business_name.empty")
      if (name.isEmpty) emptyName else Valid
    }
  )

  val nameTooLong: Constraint[String] = constraint[String](
    name => {
      lazy val tooLong = ErrorMessageFactory.error("error.business_name.maxLength")
      if (name.trim.length > nameMaxLength) tooLong else Valid
    }
  )

  val nameInvalid: Constraint[String] = constraint[String](
    name => {
      lazy val invalidName = ErrorMessageFactory.error("error.business_name.invalid")
      if (Patterns.validText(name.trim)) Valid else invalidName
    }
  )

  val dateEmpty: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error("error.date.empty")
      if (date.day.trim.isEmpty && date.month.trim.isEmpty && date.year.trim.isEmpty) emptyDate else Valid
    }
  )

  val validateNino: String => Boolean = (nino: String) => nino.matches(ninoRegex)

  val clientDetailsValidationForm = Form(
    mapping(
      clientFirstName -> oText.toText.verifying(nameEmpty andThen nameTooLong andThen nameInvalid),
      clientLastName -> oText.toText.verifying(nameEmpty andThen nameTooLong andThen nameInvalid),
      clientNino -> oText.toText.verifying(validateNino),
      clientDateOfBirth -> dateMapping.verifying(dateEmpty)
    )(ClientDetailsModel.apply)(ClientDetailsModel.unapply)
  )

  val clientDetailsForm = PreprocessedForm(clientDetailsValidationForm)

}
