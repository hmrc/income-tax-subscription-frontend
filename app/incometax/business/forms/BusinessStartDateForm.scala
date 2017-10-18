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

package incometax.business.forms

import java.time.LocalDate

import core.forms.submapping.DateMapping.dateMapping
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.models.TargetIds
import core.forms.validation.utils.ConstraintUtil._
import incometax.business.models.BusinessStartDateModel
import models.DateModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid, ValidationResult}

import scala.util.Try

object BusinessStartDateForm {

  val minStartDate: LocalDate = DateModel("06", "04", "2017").toLocalDate
  val startDate: String = "startDate"

  val dateValidation: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val invalidDate = ErrorMessageFactory.error("error.date.invalid")
      Try[ValidationResult] {
        date.toLocalDate
        Valid
      }.getOrElse(invalidDate)
    }
  )

  val dateEmpty: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error("error.date.empty")
      if (date.day.trim.isEmpty && date.month.trim.isEmpty && date.year.trim.isEmpty) emptyDate else Valid
    }
  )

  val startDateBeforeApr17: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val invalid = ErrorMessageFactory.error(TargetIds(startDate), "error.business_accounting_period.minStartDate")
      if (date.isBefore(minStartDate)) invalid else Valid
    }
  )

  val businessStartDateForm = Form(
    mapping(
      startDate -> dateMapping.verifying(dateEmpty andThen dateValidation andThen startDateBeforeApr17)
    )(BusinessStartDateModel.apply)(BusinessStartDateModel.unapply)
  )
}
