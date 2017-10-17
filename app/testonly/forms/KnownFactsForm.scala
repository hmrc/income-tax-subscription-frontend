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

package testonly.forms

import core.forms.prevalidation.PreprocessedForm
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil._
import core.forms.validation.utils.MappingUtil._
import core.forms.validation.utils.Patterns
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}
import testonly.models.KnownFactsModel

object KnownFactsForm {
  val mtdid = "mtdid"
  val nino = "nino"

  val ninoEmpty: Constraint[String] = constraint[String](
    nino => {
      lazy val emptyNino = ErrorMessageFactory.error("You must enter a nino")
      if (nino.isEmpty) emptyNino else Valid
    }
  )

  val ninoInvalid: Constraint[String] = constraint[String](
    nino => {
      import core.forms.prevalidation.trimAllFunc
      lazy val invalidNino = ErrorMessageFactory.error("You must enter a valid nino")
      if (Patterns.validNino(trimAllFunc(nino).toUpperCase())) Valid else invalidNino
    }
  )

  val mtdidEmpty: Constraint[String] = constraint[String](
    mtdid => {
      lazy val emptyMtdid = ErrorMessageFactory.error("You must enter a MTD-ID")
      if (mtdid.isEmpty) emptyMtdid else Valid
    }
  )

  val mtdidInvalid: Constraint[String] = constraint[String](
    mtdid => {
      import core.forms.prevalidation.trimAllFunc
      lazy val invalidMtdid = ErrorMessageFactory.error("You must enter a valid MTD-ID")
      val id = trimAllFunc(mtdid)
      id.length match {
        case 15 | 16 => Valid
        case _ => invalidMtdid
      }
    }
  )

  val knownFactsValidationForm = Form(
    mapping(
      nino -> oText.toText.verifying(ninoEmpty andThen ninoInvalid),
      mtdid -> oText.toText.verifying(mtdidEmpty andThen mtdidInvalid)
    )(KnownFactsModel.apply)(KnownFactsModel.unapply)
  )

  val knownFactsForm = PreprocessedForm(knownFactsValidationForm)
}
