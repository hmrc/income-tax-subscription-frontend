/*
 * Copyright 2026 HM Revenue & Customs
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

package forms.common

import forms.validation.Constraints.{validateCharAgainst, maxLength, minLettersLength, nonEmpty}
import play.api.data.Form
import play.api.data.Forms.*
import play.api.data.validation.Constraint
import forms.validation.utils.ConstraintUtil.*
import forms.validation.utils.MappingUtil.trimmedText

object DisplayNameForm {

  val displayName: String = "business-display-name"
  val pageIdentifier: String = "display-name"

  private val displayNameAndTradeAllowedCharacters = """^[A-Za-z0-9 ,.&'\\/-]*$"""
  private val displayNameMaxLength: Int = 35
  private val displayNameMinLength: Int = 2

  private val nameNotEmpty: Constraint[String] =
    nonEmpty(s"error.$pageIdentifier.$displayName.empty")
  private val nameMaxLength: Constraint[String] =
    maxLength(displayNameMaxLength, s"error.$pageIdentifier.$displayName.max-length")
  private val nameMinLength: Constraint[String] =
    minLettersLength(displayNameMinLength, s"error.$pageIdentifier.$displayName.min-length")
  private val nameValidChars: Constraint[String] =
    validateCharAgainst(displayNameAndTradeAllowedCharacters, s"error.$pageIdentifier.$displayName.invalid")

  private val displayNameMapping =
    trimmedText.verifying(
      nameNotEmpty andThen
        nameMaxLength andThen
        nameValidChars andThen
        nameMinLength)

  val displayNameForm: Form[Seq[String]] =
    Form(displayName -> seq(displayNameMapping))
  

  def populateNameData(maybeDisplayNames: Seq[Option[String]]): Map[String, String] =
    maybeDisplayNames.zipWithIndex.map {
      case (maybeName, index) => s"$displayName[$index]" -> maybeName.getOrElse("")
    }.toMap

  def validateDisplayNames(form: Form[Seq[String]], expectedCount: Int): Form[Seq[String]] =
    form.value match {
      case Some(displayNames) if displayNames.size != expectedCount =>
        form.withError(displayName, "error.display-name.count")
      case Some(displayNames) =>
        val duplicateIndexes = displayNames.indices.filter { index =>
          displayNames.count(_ == displayNames(index)) > 1
        }
        duplicateIndexes.foldLeft(form) { (formWithErrors, index) =>
          formWithErrors.withError(
            s"$displayName[$index]",
            "error.display-name.duplicate"
          )
        }
      case None => form
    }

}
