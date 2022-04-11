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

  sealed trait FieldValidationError {
    def field: Option[DateField]
  }

  class FieldValidationErrorObject(val field: Option[DateField]) extends FieldValidationError

  final case object EmptyDay extends FieldValidationErrorObject(Some(DayField))

  final case object EmptyMonth extends FieldValidationErrorObject(Some(MonthField))

  final case object EmptyYear extends FieldValidationErrorObject(Some(YearField))

  final case object InvalidDay extends FieldValidationErrorObject(Some(DayField))

  final case object InvalidMonth extends FieldValidationErrorObject(Some(MonthField))

  final case object InvalidYear extends FieldValidationErrorObject(Some(YearField))

  final case object InvalidDate extends FieldValidationErrorObject(None)

  final case object InvalidYearLength extends FieldValidationErrorObject(Some(YearField))

  final case object TooEarly extends FieldValidationErrorObject(None)

  final case object TooLate extends FieldValidationErrorObject(None)

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
    (collectInvalidErrors(errors), collectEmptyErrors(errors), collectLengthErrors(errors)) match {
      case (Nil, Nil, Nil) =>
        collectDateErrors(errors, ids, isAgent, errorContext, minDate, maxDate, dateFormatter)
      case (Nil, Nil, fields) =>
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.length")
      case (Nil, fields, Nil) =>
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.empty")
      case (fields, Nil, Nil) =>
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
      case (a, b, c) =>
        val fields = a ++ b ++ c
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"date.invalid")
    }
  }

  private def fieldsToMessageKey(fields: List[DateField]): String = fields.toSet.toList.sorted.mkString("_")

  private def fieldsToFormKey(fields: List[DateField], ids: HtmlIds) = {
    (fields.contains(DayField), fields.contains(MonthField), fields.contains(YearField)) match {
      case (true, _, _) => ids.totalDayKey
      case (_, true, _) => ids.totalMonthKey
      case (_, _, true) => ids.totalYearKey
      case _ => ids.totalDayKey
    }
  }

  private def collectInvalidErrors(errors: List[FieldValidationError]): List[DateField] = collectFields(errors, InvalidDay, InvalidMonth, InvalidYear)

  private def collectEmptyErrors(errors: List[FieldValidationError]): List[DateField] = collectFields(errors, EmptyDay, EmptyMonth, EmptyYear)

  private def collectLengthErrors(errors: List[FieldValidationError]): List[DateField] = collectFields(errors, InvalidYearLength)

  private def collectDateErrors(errors: List[FieldValidationError],
                                ids: HtmlIds,
                                isAgent: Boolean = false,
                                errorContext: String,
                                minDate: Option[LocalDate] = None,
                                maxDate: Option[LocalDate] = None,
                                dateFormatter: Option[LocalDate => String]): DateModelValidation = {
    val formatter: LocalDate => String = dateFormatter.getOrElse(defaultDateFormatter)

    if (errors.contains(TooEarly)) {
      error(
        isAgent,
        errorContext,
        ids.totalDayKey,
        dateTooEarly,
        minDate.fold(List.empty[String])(value => List(formatter(value)))
      )
    } else if (errors.contains(TooLate)) {
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

  private def collectFields(errors: List[FieldValidationError], errorsToFind: FieldValidationError*): List[DateField] = {
    errorsToFind.foldLeft[List[DateField]](Nil)(
      (acc, errorToFind) => if (errors.contains(errorToFind)) acc ++ errorToFind.field else acc
    )
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
