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

package forms.individual.business

import forms.prevalidation.{PreprocessedForm, PrevalidationAPI}
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import forms.validation.utils.Patterns
import models.common.BusinessNameModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

object BusinessNameForm {

  val businessName = "businessName"

  val businessNameMaxLength = 105

  val nameNotEmpty: Constraint[String] = constraint[String](
    name => {
      lazy val emptyName = Invalid("error.business_name.empty")
      if (name.isEmpty) emptyName else Valid
    }
  )

  val nameNotTooLong: Constraint[String] = constraint[String](
    name => {
      lazy val tooLong = Invalid("error.business_name.maxLength")
      if (name.trim.length > businessNameMaxLength) tooLong else Valid
    }
  )

  val nameHasValidChars: Constraint[String] = constraint[String](
    name => {
      lazy val invalidName = Invalid("error.business_name.invalid")
      if (Patterns.validText(name.trim)) Valid else invalidName
    }
  )

  def nameIsNotExcluded(excludedNames: Seq[BusinessNameModel]): Constraint[String] = constraint[String] { name =>
    if (excludedNames.exists(_.businessName == name)) Invalid("error.business_trade_name.duplicate")
    else Valid
  }

  def businessNameValidationForm(excludedBusinessNames: Seq[BusinessNameModel] = Seq()): Form[BusinessNameModel] = Form(
    mapping(
      businessName -> trimmedText.verifying(
        nameNotEmpty andThen nameNotTooLong andThen nameHasValidChars andThen nameIsNotExcluded(excludedBusinessNames)
      )
    )(BusinessNameModel.apply)(BusinessNameModel.unapply)
  )

  def businessNameForm(excludedBusinessNames: Seq[BusinessNameModel] = Seq()): PrevalidationAPI[BusinessNameModel] =
    PreprocessedForm(businessNameValidationForm(excludedBusinessNames))
}
