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

package agent.forms

import agent.forms.validation.ErrorMessageFactory
import agent.forms.validation.utils.ConstraintUtil._
import agent.forms.validation.utils.MappingUtil._
import agent.forms.validation.utils.Patterns
import agent.models.EmailModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object EmailForm {

  val emailAddress = "emailAddress"
  val emailMaxLength = 70

  val emailEmpty: Constraint[String] = constraint[String](
    email => {
      lazy val emptyName = ErrorMessageFactory.error("error.contact_email.empty")
      if (email.isEmpty) emptyName else Valid
    }
  )

  val emailTooLong: Constraint[String] = constraint[String](
    email => {
      lazy val tooLong = ErrorMessageFactory.error("error.contact_email.maxLength")
      if (email.trim.length > emailMaxLength) tooLong else Valid
    }
  )

  val emailInvalid: Constraint[String] = constraint[String](
    email => {
      lazy val invalidName = ErrorMessageFactory.error("error.contact_email.invalid")
      if (Patterns.validEmail(email.trim)) Valid else invalidName
    }
  )

  val emailForm = Form(
    mapping(
      emailAddress -> oText.toText.verifying(emailEmpty andThen emailTooLong andThen emailInvalid)
    )(EmailModel.apply)(EmailModel.unapply)
  )

}
