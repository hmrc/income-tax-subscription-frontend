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

import core.forms.prevalidation.PreprocessedForm
import core.forms.submapping.DateMapping.dateMapping
import core.forms.validation.Constraints._
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil._
import core.forms.validation.utils.MappingUtil._
import core.models.DateModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid, ValidationResult}
import usermatching.models.UserDetailsModel

import scala.util.Try

object ClientDetailsForm {

  val clientFirstName = "clientFirstName"
  val clientLastName = "clientLastName"
  val clientNino = "clientNino"
  val clientDateOfBirth = "clientDateOfBirth"

  val nameMaxLength = 105

  val firstNameNonEmpty: Constraint[String] = nonEmpty("agent.error.client_details.first_name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("agent.error.client_details.last_name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("agent.error.client_details.first_name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("agent.error.client_details.last_name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "agent.error.client_details.first_name.maxLength")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "agent.error.client_details.last_name.maxLength")

  val emptyClientNino: Constraint[String] = nonEmpty("agent.error.nino.empty")

  val validateClientNino: Constraint[String] = {
    constraint[String](nino => if (nino.filterNot(_.isWhitespace).matches(ninoRegex)) Valid else ErrorMessageFactory.error("agent.error.nino.invalid"))
  }

  val dobNoneEmpty: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error("agent.error.dob_date.empty")
      if (date.day.trim.isEmpty && date.month.trim.isEmpty && date.year.trim.isEmpty) emptyDate else Valid
    }
  )

  val dobIsNumeric: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val isNotNumeric = ErrorMessageFactory.error("agent.error.dob_date.invalid_chars")
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
      }.getOrElse(ErrorMessageFactory.error("agent.error.dob_date.invalid"))
    }
  )


  val clientDetailsValidationForm = Form(
    mapping(
      clientFirstName -> oText.toText.verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      clientLastName -> oText.toText.verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      clientNino -> oText.toText.verifying(emptyClientNino andThen validateClientNino),
      clientDateOfBirth -> dateMapping.verifying(dobNoneEmpty andThen dobIsNumeric andThen dobInvalid)
    )(UserDetailsModel.apply)(UserDetailsModel.unapply)
  )

  import core.forms.prevalidation.CaseOption._
  import core.forms.prevalidation.TrimOption._

  val clientDetailsForm = PreprocessedForm(
    validation = clientDetailsValidationForm,
    trimRules = Map(clientNino -> bothAndCompress),
    caseRules = Map(clientNino -> upper)
  )

}
