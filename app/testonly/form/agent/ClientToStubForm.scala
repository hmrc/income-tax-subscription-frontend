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

package testonly.form.agent

import forms.prevalidation.CaseOption._
import forms.prevalidation.{PreprocessedForm, PrevalidationAPI}
import forms.prevalidation.TrimOption._
import forms.submapping.DateMapping.dateMapping
import forms.validation.Constraints._
import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import models.DateModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid, ValidationResult}
import testonly.models.agent.ClientToStubModel

import scala.util.Try

object ClientToStubForm {

  val clientFirstName = "clientFirstName"
  val clientLastName = "clientLastName"
  val clientNino = "clientNino"
  val clientSautr = "clientSautr"
  val clientDateOfBirth = "clientDateOfBirth"

  val nameMaxLength = 105

  val firstNameNonEmpty: Constraint[String] = nonEmpty("error.client_details.first_name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("error.client_details.last_name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("error.client_details.first_name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("error.client_details.last_name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.client_details.first_name.maxLength")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.client_details.last_name.maxLength")

  val emptyUtr: Constraint[String] = nonEmpty("You must enter an SA UTR")

  val utrRegex = """^(?:[ \t]*\d[ \t]*){10}$"""

  val validateUtr: Constraint[String] =
    constraint[String](utr =>
      if (utr.filterNot(_.isWhitespace).matches(utrRegex)) Valid
      else ErrorMessageFactory.error("You must enter a valid SA UTR")
    )

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

  val clientDetailsValidationForm = Form(
    mapping(
      clientFirstName -> oText.toText.verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      clientLastName -> oText.toText.verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      clientNino -> oText.toText.verifying(emptyNino andThen validateNino),
      clientSautr -> oText.toText.verifying(emptyUtr andThen validateUtr),
      clientDateOfBirth -> dateMapping.verifying(dobEmpty andThen dobValidation)
    )(ClientToStubModel.apply)(ClientToStubModel.unapply)
  )



  val clientToStubForm: PrevalidationAPI[ClientToStubModel] = PreprocessedForm(
    validation = clientDetailsValidationForm,
    trimRules = Map(clientNino -> bothAndCompress),
    caseRules = Map(clientNino -> upper)
  )

}
