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

package incometax.business.forms

import core.forms.submapping.DateMapping._
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil._
import core.models.DateModel
import incometax.business.models.AccountingPeriodModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Valid, ValidationResult}

import scala.util.Try

object AccountingPeriodDateForm {

  val startDate: String = "startDate"
  val endDate: String = "endDate"

  def dateValidation(errorName: String): Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val invalidDate = ErrorMessageFactory.error(s"error.$errorName.invalid")
      Try[ValidationResult] {
        date.toLocalDate
        Valid
      }.getOrElse(invalidDate)
    }
  )

  def dateEmpty(errorName: String): Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val emptyDate = ErrorMessageFactory.error(s"error.$errorName.empty")
      if (date.day.trim.isEmpty && date.month.trim.isEmpty && date.year.trim.isEmpty) emptyDate else Valid
    }
  )

  def dateIsNumeric(errorName: String): Constraint[DateModel] = constraint[DateModel](
    date => {
      lazy val isNotNumeric = ErrorMessageFactory.error(s"error.$errorName.invalid_chars")
      val numericRegex = "[0-9]*"

      def isNumeric(str: String): Boolean = str.replace(" ", "").matches(numericRegex)

      if (isNumeric(date.day) && isNumeric(date.month) && isNumeric(date.year)) Valid else isNotNumeric
    }
  )


  val startDateConstraints = {
    val name = "start_date"
    dateEmpty(name) andThen dateIsNumeric(name) andThen dateValidation(name)
  }

  val endDateConstraints = {
    val name = "end_date"
    dateEmpty(name) andThen dateIsNumeric(name) andThen dateValidation(name)
  }

  val accountingPeriodDateForm = Form(
    mapping(
      startDate -> dateMapping.verifying(startDateConstraints),
      endDate -> dateMapping.verifying(endDateConstraints)
    )(AccountingPeriodModel.apply)(AccountingPeriodModel.unapply)
  )

}
