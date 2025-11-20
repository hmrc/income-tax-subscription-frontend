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

package testonly.form.individual

import forms.formatters.LocalDateMapping
import forms.validation.Constraints._
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import models.DateModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import testonly.models.UserToStubModel

object UserToStubForm extends LocalDateMapping {

  val userFirstName = "userFirstName"
  val userLastName = "userLastName"
  val userNino = "userNino"
  val userSautr = "userSautr"
  val userDateOfBirth = "userDateOfBirth"

  val nameMaxLength = 105

  val errorMessage = "error.message"

  val firstNameNonEmpty: Constraint[String] = nonEmpty("error.user-details.first-name.empty")
  val lastNameNonEmpty: Constraint[String] = nonEmpty("error.user-details.last-name.empty")

  val firstNameInvalid: Constraint[String] = invalidFormat("error.user-details.first-name.invalid")
  val lastNameInvalid: Constraint[String] = invalidFormat("error.user-details.last-name.invalid")

  val firstNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user-details.first-name.max-length")
  val lastNameMaxLength: Constraint[String] = maxLength(nameMaxLength, "error.user-details.last-name.max-length")

  val utrRegex = """^(?:[ \t]*\d[ \t]*){10}$"""

  val validateUtr: Constraint[Option[String]] =
    constraint[Option[String]] {
      case Some(utr) =>
        if (utr.filterNot(_.isWhitespace).matches(utrRegex)) Valid
        else Invalid("You must enter a valid SA UTR")
      case _ => Valid
    }

  def userDetailsForm(implicit messages: Messages): Form[UserToStubModel] = Form(
    mapping(
      userFirstName -> oText.toText.verifying(firstNameNonEmpty andThen firstNameMaxLength andThen firstNameInvalid),
      userLastName -> oText.toText.verifying(lastNameNonEmpty andThen lastNameMaxLength andThen lastNameInvalid),
      userNino -> oText.toText.verifying(emptyNino andThen validateNino),
      userSautr -> oText.verifying(validateUtr),
      userDateOfBirth -> localDate(
        invalidKey = errorMessage,
        allRequiredKey = errorMessage,
        twoRequiredKey = errorMessage,
        requiredKey = errorMessage,
        invalidYearKey = errorMessage)
        .transform(DateModel.dateConvert, DateModel.dateConvert)
    )(UserToStubModel.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

}
