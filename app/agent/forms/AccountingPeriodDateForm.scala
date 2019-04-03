/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import core.forms.submapping.DateMapping._
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.models.TargetIds
import core.forms.validation.utils.ConstraintUtil._
import core.models.DateModel
import incometax.business.models.AccountingPeriodModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid, ValidationResult}

import scala.util.Try

object AccountingPeriodDateForm {

  val minStartDate: LocalDate = LocalDate.now
  val maxMonths: Int = 24
  val startDate: String = "startDate"
  val endDate: String = "endDate"

  def dateValidation(errorName: String): Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val invalidDate = ErrorMessageFactory.error(s"agent.error.$errorName.invalid")
      Try[ValidationResult] {
        date.toLocalDate
        Valid
      }.getOrElse(invalidDate)
    }
  )

  def dateEmpty(errorName: String): Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error(s"agent.error.$errorName.empty")
      if (date.day.trim.isEmpty && date.month.trim.isEmpty && date.year.trim.isEmpty) emptyDate else Valid
    }
  )

  def dateIsNumeric(errorName: String): Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val isNotNumeric = ErrorMessageFactory.error(s"agent.error.$errorName.invalid_chars")
      val numericRegex = "[0-9]*"

      def isNumeric(str: String): Boolean = str.replace(" ", "").matches(numericRegex)

      if (isNumeric(date.day) && isNumeric(date.month) && isNumeric(date.year)) Valid else isNotNumeric
    }
  )

  val startDateBeforeApr17: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val invalid = ErrorMessageFactory.error(TargetIds(startDate), "agent.error.business_accounting_period.minStartDate")
      if (DateModel.dateConvert(date).isBefore(minStartDate)) invalid else Valid
    }
  )

  val endDateAfterStart: Constraint[AccountingPeriodModel] = constraint[AccountingPeriodModel](
    accountingPeriod => {
      lazy val invalid = ErrorMessageFactory.error(TargetIds(endDate), "agent.error.end_date_violation")
      if (DateModel.dateConvert(accountingPeriod.endDate).isAfter(DateModel.dateConvert(accountingPeriod.startDate))) Valid else invalid
    }
  )

  val presentOrFutureDate: Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val invalid = ErrorMessageFactory.error(TargetIds(endDate), "agent.error.end_date_past")
      if (DateModel.dateConvert(date).isBefore(LocalDate.now())) invalid else Valid
    }
  )

  val endDate24MonthRule: Constraint[AccountingPeriodModel] = constraint[AccountingPeriodModel](
    accountingPeriod => {
      lazy val maxEndDate = DateModel.dateConvert(accountingPeriod.startDate).plusMonths(maxMonths).minusDays(1)
      lazy val invalid = ErrorMessageFactory.error(
        TargetIds(endDate),
        "agent.error.business_accounting_period.maxEndDate"
      )
      if (DateModel.dateConvert(accountingPeriod.endDate).isAfter(maxEndDate)) invalid else Valid
    }
  )

  val startDateConstraints = {
    val name = "start_date"
    dateEmpty(name) andThen dateIsNumeric(name) andThen dateValidation(name) andThen startDateBeforeApr17
  }

  val endDateConstraints = {
    val name = "end_date"
    dateEmpty(name) andThen dateIsNumeric(name) andThen dateValidation(name) andThen presentOrFutureDate
  }

  val accountingPeriodDateForm = Form(
    mapping(
      startDate -> dateMapping.verifying(startDateConstraints),
      endDate -> dateMapping.verifying(endDateConstraints)
    )(AccountingPeriodModel.apply)(AccountingPeriodModel.unapply).verifying(endDateAfterStart andThen endDate24MonthRule)
  )

}
