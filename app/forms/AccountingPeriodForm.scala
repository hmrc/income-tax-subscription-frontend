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

package forms

import forms.submapping.DateMapping._
import models.{AccountingPeriodModel, DateModel}
import play.api.data.Form
import play.api.data.Forms.mapping
import forms.validation.util.ConstraintUtil._
import forms.validation.ErrorMessageFactory
import forms.validation.models.TargetIds
import play.api.data.validation.{Constraint, Valid, ValidationResult}

import scala.util.Try

object AccountingPeriodForm {

  val startDate = "startDate"
  val endDate = "endDate"

  val dateValidation: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val invalidDate = ErrorMessageFactory.error("error.invalid_date")
      Try[ValidationResult] {
        date.toLocalDate
        Valid
      }.getOrElse(invalidDate)
    }
  )

  val dateEmpty: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error("error.empty_date")
      if (date.day.isEmpty && date.month.isEmpty && date.year.isEmpty) emptyDate else Valid
    }
  )

  val endDateAfterStart: Constraint[AccountingPeriodModel] = constraint[AccountingPeriodModel](
    accountingPeriod => {
      lazy val invalid = ErrorMessageFactory.error(TargetIds(endDate),"error.end_date_violation")
      if (accountingPeriod.endDate.isAfter(accountingPeriod.startDate)) Valid else invalid
    }
  )

  val accountingPeriodForm = Form(
    mapping(
      startDate -> dateMapping.verifying(dateEmpty andThen dateValidation),
      endDate -> dateMapping.verifying(dateEmpty andThen dateValidation)
    )(AccountingPeriodModel.apply)(AccountingPeriodModel.unapply).verifying(endDateAfterStart)
  )

}
