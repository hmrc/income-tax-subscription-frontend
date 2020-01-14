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

package forms.usermatching

import forms.submapping.DateMapping.dateMapping
import forms.validation.Constraints._
import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import forms.prevalidation.PreprocessedForm
import core.models.DateModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid, ValidationResult}
import usermatching.models.UserDetailsModel

import scala.util.Try

object UserDetailsForm {

  val userFirstName = "userFirstName"
  val userLastName = "userLastName"
  val userNino = "userNino"
  val userDateOfBirth = "userDateOfBirth"

  val nameMaxLength = 105

  val firstNameNonEmpty: Constraint[String] = nonEmpty("error.user_details.first_name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("error.user_details.last_name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("error.user_details.first_name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("error.user_details.last_name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user_details.first_name.maxLength")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user_details.last_name.maxLength")

  val dobNoneEmpty: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error("error.dob_date.empty")
      if (date.day.trim.isEmpty && date.month.trim.isEmpty && date.year.trim.isEmpty) emptyDate else Valid
    }
  )

  val dobIsNumeric: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val isNotNumeric = ErrorMessageFactory.error("error.dob_date.invalid_chars")
      val numericRegex = "[0-9]*"

      def isNumeric(str: String): Boolean = str.replace(" ","").matches(numericRegex)

      if (isNumeric(date.day) && isNumeric(date.month) && isNumeric(date.year)) Valid else isNotNumeric
    }
  )

  val dobInvalid: Constraint[DateModel] = constraint[DateModel](
    date => {
      Try[ValidationResult] {
        date.toLocalDate
        Valid
      }.getOrElse(ErrorMessageFactory.error("error.dob_date.invalid"))
    }
  )

  val userDetailsValidationForm = Form(
    mapping(
      userFirstName -> oText.toText.verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      userLastName -> oText.toText.verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      userNino -> oText.toText.verifying(emptyNino andThen validateNino),
      userDateOfBirth -> dateMapping.verifying(dobNoneEmpty andThen dobIsNumeric andThen dobInvalid)
    )(UserDetailsModel.apply)(UserDetailsModel.unapply)
  )

  import forms.prevalidation.CaseOption._
  import forms.prevalidation.TrimOption._

  val userDetailsForm = PreprocessedForm(
    validation = userDetailsValidationForm,
    trimRules = Map(userNino -> bothAndCompress),
    caseRules = Map(userNino -> upper)
  )

}
