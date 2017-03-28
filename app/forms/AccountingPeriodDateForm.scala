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

import java.time.LocalDate

import forms.submapping.DateMapping._
import forms.validation.ErrorMessageFactory
import forms.validation.models.TargetIds
import forms.validation.utils.ConstraintUtil._
import models.{AccountingPeriodModel, DateModel}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid, ValidationResult}

import scala.util.Try

object AccountingPeriodDateForm {

  val minStartDate: LocalDate = DateModel("06","04","2017").toLocalDate
  val maxMonths: Int = 24
  val startDate: String = "startDate"
  val endDate: String = "endDate"

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

  val endDateAfterStart: Constraint[AccountingPeriodModel] = constraint[AccountingPeriodModel](
    accountingPeriod => {
      lazy val invalid = ErrorMessageFactory.error(TargetIds(endDate), "error.end_date_violation")
      if (accountingPeriod.endDate.isAfter(accountingPeriod.startDate)) Valid else invalid
    }
  )

  val endDate24MonthRule: Constraint[AccountingPeriodModel] = constraint[AccountingPeriodModel](
    accountingPeriod => {
      lazy val maxEndDate = accountingPeriod.startDate.plusMonths(maxMonths)
      lazy val invalid = ErrorMessageFactory.error(
        TargetIds(endDate),
        "error.business_accounting_period.maxEndDate"
      )
      if (accountingPeriod.endDate.isAfter(maxEndDate)) invalid else Valid
    }
  )

  val accountingPeriodDateForm = Form(
    mapping(
      startDate -> dateMapping.verifying(dateEmpty andThen dateValidation andThen startDateBeforeApr17),
      endDate -> dateMapping.verifying(dateEmpty andThen dateValidation)
    )(AccountingPeriodModel.apply)(AccountingPeriodModel.unapply).verifying(endDateAfterStart andThen endDate24MonthRule)
  )

}
