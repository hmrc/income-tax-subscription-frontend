/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.formatters.NewDateModelMapping
import forms.prevalidation.{PreprocessedForm, PrevalidationAPI}
import forms.validation.Constraints._
import forms.validation.utils.ConstraintUtil._
import models.DateModel
import models.usermatching.UserDetailsModel
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate

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

  val dateInPast: Constraint[DateModel] = constraint[DateModel] { dateModel =>
    if (dateModel.toLocalDate.isBefore(LocalDate.now)) {
      Valid
    } else {
      Invalid("error.user_details.date_of_birth.not_in_past")
    }
  }

  val userDetailsValidationForm: Form[UserDetailsModel] = Form(
    mapping(
      userFirstName -> default(text, "").verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      userLastName -> default(text, "").verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      userNino -> default(text, "").verifying(emptyNino andThen validateNino),
      userDateOfBirth -> NewDateModelMapping.dateModelMapping(isAgent = false, errorContext = "user_details.date_of_birth", None, None, None).verifying(dateInPast)
    )(UserDetailsModel.apply)(UserDetailsModel.unapply)
  )

  import forms.prevalidation.CaseOption._
  import forms.prevalidation.TrimOption._

  val userDetailsForm: PrevalidationAPI[UserDetailsModel] = PreprocessedForm(
    validation = userDetailsValidationForm,
    trimRules = Map(userNino -> bothAndCompress),
    caseRules = Map(userNino -> upper)
  )

}