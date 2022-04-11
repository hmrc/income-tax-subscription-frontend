/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.formatters

import forms.formatters.DateModelMapping.{DateModelValidation, HtmlIds}
import forms.formatters.DateValidation.{DateField, DayField, MonthField, YearField}
import play.api.data.FormError

import java.time.LocalDate

object DateErrorMapping {
  final case class InvalidFields(fields: List[DateField])
  final case class EmptyFields(fields: List[DateField])
  final case class WrongLengthFields(fields: List[DateField])

  sealed trait FieldValidationError
  final case object EmptyDay extends FieldValidationError
  final case object EmptyMonth extends FieldValidationError
  final case object EmptyYear extends FieldValidationError
  final case object InvalidDay extends FieldValidationError
  final case object InvalidMonth extends FieldValidationError
  final case object InvalidYear extends FieldValidationError
  final case object InvalidDate extends FieldValidationError
  final case object InvalidYearLength extends FieldValidationError
  final case object TooEarly extends FieldValidationError
  final case object TooLate extends FieldValidationError

  private def invalidLength(errorKey: String) = s"$errorKey.length"
  private def empty(errorKey: String) = s"$errorKey.empty"
  private def invalid(errorKey: String) = s"$errorKey.invalid"

  private val dateTooEarly = "day_month_year.minDate"
  private val dateTooLate = "day_month_year.maxDate"
  private val emptyDate = "day_month_year.empty"

  def transformErrors(errors: List[FieldValidationError],
                      ids: HtmlIds,
                      isAgent: Boolean = false,
                      errorContext: String,
                      minDate: Option[LocalDate] = None,
                      maxDate: Option[LocalDate] = None,
                      dateFormatter: Option[LocalDate => String]): DateModelValidation = {
    (mapToInvalidError(errors), mapToEmptyError(errors), mapToYearError(errors)) match {
      case (None, None, None) => {
        mapToDateError(errors, ids, isAgent, errorContext, minDate, maxDate, dateFormatter)
      }
      case (None, None, Some(WrongLengthFields(fields))) => {
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), invalidLength(fieldsToMessageKey(fields)))
      }
      case (None, Some(EmptyFields(fields)), None) => {
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), empty(fieldsToMessageKey(fields)))
      }
      case (None, Some(EmptyFields(emptyFields)), Some(WrongLengthFields(wrongLengthFields))) => {
        val fields = emptyFields ++ wrongLengthFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), invalid(fieldsToMessageKey(fields)))
      }
      case (Some(InvalidFields(fields)), None, None) => {
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), invalid(fieldsToMessageKey(fields)))
      }
      case (Some(InvalidFields(invalidFields)), None, Some(WrongLengthFields(wrongLengthFields))) => {
        val fields = invalidFields ++ wrongLengthFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), invalid(fieldsToMessageKey(fields)))
      }
      case (Some(InvalidFields(invalidFields)), Some(EmptyFields(emptyFields)), None) => {
        val fields = invalidFields ++ emptyFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), invalid(fieldsToMessageKey(invalidFields ++ emptyFields)))
      }
      case (Some(InvalidFields(invalidFields)), Some(EmptyFields(emptyFields)), Some(WrongLengthFields(wrongLengthFields))) => {
        val fields = invalidFields ++ emptyFields ++ wrongLengthFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), invalid(fieldsToMessageKey(fields)))
      }
    }
  }

  private def fieldsToFormKey(fields: List[DateField], ids: HtmlIds) = {
    (fields.contains(DayField), fields.contains(MonthField), fields.contains(YearField)) match {
      case (true, _, _) => ids.totalDayKey
      case (false, true, _) => ids.totalMonthKey
      case (false, false, true) => ids.totalYearKey
      case _ => ids.totalDayKey
    }
  }

  private def fieldsToMessageKey(fields: List[DateField]): String =
    fields.toSet.toList.sorted.mkString("_")

  private def mapToInvalidError(errors: List[FieldValidationError]): Option[InvalidFields] = {
    val fields = collectFields(errors.contains(InvalidDay), errors.contains(InvalidMonth), errors.contains(InvalidYear))
    if(fields.isEmpty) None else Some(InvalidFields(fields))
  }

  private def mapToEmptyError(errors: List[FieldValidationError]): Option[EmptyFields] = {
    val fields = collectFields(errors.contains(EmptyDay), errors.contains(EmptyMonth), errors.contains(EmptyYear))
    if(fields.isEmpty) None else Some(EmptyFields(fields))
  }

  private def mapToYearError(errors: List[FieldValidationError]): Option[WrongLengthFields] = {
    if(errors.contains(InvalidYearLength)) {
      Some(WrongLengthFields(List(YearField)))
    } else {
      None
    }
  }

  private def mapToDateError(errors: List[FieldValidationError],
                             ids: HtmlIds,
                             isAgent: Boolean = false,
                             errorContext: String,
                             minDate: Option[LocalDate] = None,
                             maxDate: Option[LocalDate] = None,
                             dateFormatter: Option[LocalDate => String]): DateModelValidation = {
    val formatter: LocalDate => String = dateFormatter.getOrElse(defaultDateFormatter)

    if(errors.contains(TooEarly)) {
      error(
        isAgent,
        errorContext,
        ids.totalDayKey,
        dateTooEarly,
        minDate.fold(List.empty[String])(value => List(formatter(value)))
      )
    } else if(errors.contains(TooLate)) {
      error(
        isAgent,
        errorContext,
        ids.totalDayKey,
        dateTooLate,
        maxDate.fold(List.empty[String])(value => List(formatter(value)))
      )
    } else {
      error(
        isAgent,
        errorContext,
        ids.totalDayKey,
        emptyDate
      )
    }
  }

  private def collectFields(containsDay: Boolean, containsMonth: Boolean, containsYear: Boolean): List[DateField] = {
    (containsDay, containsMonth, containsYear) match {
      case (false, false, true) => List(YearField)
      case (false, true, false) => List(MonthField)
      case (false, true, true) => List(MonthField, YearField)
      case (true, false, false) => List(DayField)
      case (true, false, true) => List(DayField, YearField)
      case (true, true, false) => List(DayField, MonthField)
      case (true, true, true) => List(DayField, MonthField, YearField)
      case _ => List.empty[DateField]
    }
  }

  private def errorKey(isAgent: Boolean = false,
                       errorContext: String,
                       error: String): String =
    if (isAgent) s"agent.error.$errorContext.$error" else s"error.$errorContext.$error"

  private def error(isAgent: Boolean = false,
                    errorContext: String,
                    id: String, errorKeyName: String,
                    args: List[Any] = Nil) =
    Left(Seq(FormError(id, errorKey(isAgent, errorContext, errorKeyName), args)))

  private def defaultDateFormatter: LocalDate => String = d => d.toString
}
