/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.formatters.LocalDateMapping
import forms.validation.Constraints.{invalidFormat, maxLength, ninoRegex, nonEmpty}
import forms.validation.utils.ConstraintUtil._
import models.DateModel
import models.usermatching.UserDetailsModel
import play.api.data.Form
import play.api.data.Forms.{default, mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.LocalDate

object ClientDetailsForm extends LocalDateMapping {

  val clientFirstName = "clientFirstName"
  val clientLastName = "clientLastName"
  val clientNino = "clientNino"
  val clientDateOfBirth = "clientDateOfBirth"

  val nameMaxLength = 105

  val dobErrorContext: String = "client-details.date-of-birth"

  val firstNameNonEmpty: Constraint[String] = nonEmpty("agent.error.client-details.first-name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("agent.error.client-details.last-name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("agent.error.client-details.first-name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("agent.error.client-details.last-name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "agent.error.client-details.first-name.max-length")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "agent.error.client-details.last-name.max-length")

  val emptyClientNino: Constraint[String] = nonEmpty("agent.error.nino.empty")

  val validateClientNino: Constraint[String] = {
    constraint[String](nino => if (nino.filterNot(_.isWhitespace).matches(ninoRegex)) Valid else Invalid("agent.error.nino.invalid"))
  }

  val dateInPast: Constraint[DateModel] = constraint[DateModel] { dateModel =>
    if (dateModel.toLocalDate.isBefore(LocalDate.now)) {
      Valid
    } else {
      Invalid(s"agent.error.$dobErrorContext.day-month-year.not-in-past")
    }
  }

  def clientDetailsForm(implicit messages: Messages): Form[UserDetailsModel] = Form[UserDetailsModel](
    mapping(
      clientFirstName -> default(text, "").verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      clientLastName -> default(text, "").verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      clientNino -> default(text, "").transform[String](_.filterNot(_.isWhitespace).toUpperCase, identity).verifying(emptyClientNino andThen validateClientNino),
      clientDateOfBirth -> localDate(
        invalidKey = s"agent.error.$dobErrorContext.invalid",
        allRequiredKey = s"agent.error.$dobErrorContext.all.empty",
        twoRequiredKey = s"agent.error.$dobErrorContext.required.two",
        requiredKey = s"agent.error.$dobErrorContext.required",
        invalidYearKey = s"agent.error.$dobErrorContext.year.length")
        .transform(DateModel.dateConvert, DateModel.dateConvert).verifying(dateInPast)
    )(UserDetailsModel.apply)(o => Some(Tuple.fromProductTyped(o)))

  )
}
