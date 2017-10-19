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

package incometax.business.forms

import core.forms.prevalidation.PreprocessedForm
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil._
import core.forms.validation.utils.MappingUtil._
import core.forms.validation.utils.Patterns
import incometax.business.models.BusinessNameModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object BusinessNameForm {

  val businessName = "businessName"

  val businessNameMaxLength = 105

  val nameEmpty: Constraint[String] = constraint[String](
    name => {
      lazy val emptyName = ErrorMessageFactory.error("error.business_name.empty")
      if (name.isEmpty) emptyName else Valid
    }
  )

  val nameTooLong: Constraint[String] = constraint[String](
    name => {
      lazy val tooLong = ErrorMessageFactory.error("error.business_name.maxLength")
      if (name.trim.length > businessNameMaxLength) tooLong else Valid
    }
  )

  val nameInvalid: Constraint[String] = constraint[String](
    name => {
      lazy val invalidName = ErrorMessageFactory.error("error.business_name.invalid")
      if (Patterns.validText(name.trim)) Valid else invalidName
    }
  )

  val businessNameValidationForm = Form(
    mapping(
      businessName -> oText.toText.verifying(nameEmpty andThen nameTooLong andThen nameInvalid)
    )(BusinessNameModel.apply)(BusinessNameModel.unapply)
  )

  val businessNameForm = PreprocessedForm(businessNameValidationForm)
}
