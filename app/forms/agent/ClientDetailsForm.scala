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

package forms.agent

import forms.formatters.DateModelMapping
import forms.prevalidation.{PreprocessedForm, PrevalidationAPI}
import forms.validation.Constraints.{invalidFormat, maxLength, ninoRegex, nonEmpty}
import forms.validation.utils.ConstraintUtil._
import models.DateModel
import models.usermatching.UserDetailsModel
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate

object ClientDetailsForm {

  val clientFirstName = "clientFirstName"
  val clientLastName = "clientLastName"
  val clientNino = "clientNino"
  val clientDateOfBirth = "clientDateOfBirth"

  val nameMaxLength = 105

  val errorContext: String = "dob_date"

  val firstNameNonEmpty: Constraint[String] = nonEmpty("agent.error.client_details.first_name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("agent.error.client_details.last_name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("agent.error.client_details.first_name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("agent.error.client_details.last_name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "agent.error.client_details.first_name.maxLength")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "agent.error.client_details.last_name.maxLength")

  val emptyClientNino: Constraint[String] = nonEmpty("agent.error.nino.empty")

  val validateClientNino: Constraint[String] = {
    constraint[String](nino => if (nino.filterNot(_.isWhitespace).matches(ninoRegex)) Valid else Invalid("agent.error.nino.invalid"))
  }

  val dateInPast: Constraint[DateModel] = constraint[DateModel] { dateModel =>
    if (dateModel.toLocalDate.isBefore(LocalDate.now)) {
      Valid
    } else {
      Invalid(s"agent.error.$errorContext.day_month_year.not_in_past")
    }
  }

  val clientDetailsValidationForm: Form[UserDetailsModel] = Form[UserDetailsModel](
    mapping(
      clientFirstName -> default(text, "").verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      clientLastName -> default(text, "").verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      clientNino -> default(text, "").verifying(emptyClientNino andThen validateClientNino),
      clientDateOfBirth -> DateModelMapping.dateModelMapping(isAgent = true, errorContext, None, None, None).verifying(dateInPast)
    )(UserDetailsModel.apply)(UserDetailsModel.unapply)
  )

  import forms.prevalidation.CaseOption._
  import forms.prevalidation.TrimOption._

  val clientDetailsForm: PrevalidationAPI[UserDetailsModel] = PreprocessedForm(
    validation = clientDetailsValidationForm,
    trimRules = Map(clientNino -> bothAndCompress),
    caseRules = Map(clientNino -> upper)
  )

}
