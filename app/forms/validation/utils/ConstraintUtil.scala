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

package forms.validation.utils

import models.DateModel
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationResult}

import java.time.LocalDate

object ConstraintUtil {

  def constraint[A](f: A => ValidationResult): Constraint[A] = Constraint[A]("")(f)

  implicit class ConstraintUtil[A](cons: Constraint[A]) {

    def andThen(newCons: Constraint[A]): Constraint[A] =
      constraint((data: A) =>
        cons.apply(data) match {
          case Valid => newCons.apply(data)
          case r => r
        }
      )
  }

  def isAfter(
    minDate: LocalDate,
    errorContext: String,
    convert: LocalDate => String
  ): Constraint[DateModel] = constraint[DateModel] { dateModel =>
    val date = minDate.minusDays(1)
    if (dateModel.toLocalDate.isAfter(date)) {
      Valid
    } else {
      Invalid(s"agent.error.$errorContext.day-month-year.min-date", convert(date))
    }
  }

  def isBefore(
    maxDate: LocalDate,
    errorContext: String,
    convert: LocalDate => String
  ): Constraint[DateModel] = constraint[DateModel] { dateModel =>
    val date = maxDate.plusDays(1)
    if (dateModel.toLocalDate.isBefore(date)) {
      Valid
    } else {
      Invalid(s"agent.error.$errorContext.day-month-year.max-date", convert(date))
    }
  }
}
