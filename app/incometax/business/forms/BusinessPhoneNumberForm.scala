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

package incometax.business.forms

import core.forms.prevalidation.PreprocessedForm
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil._
import core.forms.validation.utils.MappingUtil._
import core.forms.validation.utils.Patterns
import incometax.business.models.BusinessPhoneNumberModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object BusinessPhoneNumberForm {

  val phoneNumber = "phoneNumber"

  val phoneNumberMaxLength = 24

  val numberEmpty: Constraint[String] = constraint[String](
    number => {
      lazy val emptyNumber = ErrorMessageFactory.error("error.business_phone_number.empty")
      if (number.isEmpty) emptyNumber else Valid
    }
  )

  val numberTooLong: Constraint[String] = constraint[String](
    number => {
      lazy val tooLong = ErrorMessageFactory.error("error.business_phone_number.maxLength")
      if (number.trim.length > phoneNumberMaxLength) tooLong else Valid
    }
  )

  val numberInvalid: Constraint[String] = constraint[String](
    number => {
      lazy val invalidNumber = ErrorMessageFactory.error("error.business_phone_number.invalid")
      if (Patterns.validPhoneNumber(number.trim)) Valid else invalidNumber
    }
  )

  val businessPhoneNumberValidationForm = Form(
    mapping(
      phoneNumber -> oText.toText.verifying(numberEmpty andThen numberTooLong andThen numberInvalid)
    )(BusinessPhoneNumberModel.apply)(BusinessPhoneNumberModel.unapply)
  )

  val businessPhoneNumberForm = PreprocessedForm(businessPhoneNumberValidationForm)
}
