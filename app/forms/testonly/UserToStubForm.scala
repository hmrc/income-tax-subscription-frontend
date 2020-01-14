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

package forms.testonly

import core.models.DateModel
import forms.submapping.DateMapping.dateMapping
import forms.validation.Constraints.{emptyNino, invalidFormat, maxLength, nonEmpty}
import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil._
import forms.validation.Constraints._
import forms.prevalidation.PreprocessedForm
import forms.validation.utils.MappingUtil._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid, ValidationResult}
import testonly.models.UserToStubModel

import scala.util.Try

object UserToStubForm {

  val userFirstName = "userFirstName"
  val userLastName = "userLastName"
  val userNino = "userNino"
  val userSautr = "userSautr"
  val userDateOfBirth = "userDateOfBirth"

  val nameMaxLength = 105

  val firstNameNonEmpty: Constraint[String] = nonEmpty("error.user_details.first_name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("error.user_details.last_name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("error.user_details.first_name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("error.user_details.last_name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user_details.first_name.maxLength")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user_details.last_name.maxLength")

  val emptyUtr = nonEmpty("You must enter an SA UTR")

  val utrRegex = """^(?:[ \t]*\d[ \t]*){10}$"""

  val validateUtr =
    constraint[Option[String]] {
      case Some(utr) =>
        if (utr.filterNot(_.isWhitespace).matches(utrRegex)) Valid
        else ErrorMessageFactory.error("You must enter a valid SA UTR")
      case _ => Valid
    }

  val dobEmpty: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error("error.dob_date.empty")
      if (date.day.trim.isEmpty && date.month.trim.isEmpty && date.year.trim.isEmpty) emptyDate else Valid
    }
  )

  val dobValidation: Constraint[DateModel] = constraint[DateModel](
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
      userSautr -> oText.verifying(validateUtr),
      userDateOfBirth -> dateMapping.verifying(dobEmpty andThen dobValidation)
    )(UserToStubModel.apply)(UserToStubModel.unapply)
  )

  import forms.prevalidation.CaseOption._
  import forms.prevalidation.TrimOption._

  val userToStubForm = PreprocessedForm(
    validation = userDetailsValidationForm,
    trimRules = Map(userNino -> bothAndCompress),
    caseRules = Map(userNino -> upper)
  )

}
