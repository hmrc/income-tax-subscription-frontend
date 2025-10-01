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

package forms.formatters

import models.DateModel.{DateModelValidation, HtmlIds}
import forms.formatters.DateValidation.{DateField, DayField, MonthField, YearField}
import play.api.data.FormError

import java.time.LocalDate

object DateErrorMapping {
  abstract class FieldValidationError(val field: Option[DateField])

  final case object EmptyDay extends FieldValidationError(Some(DayField))

  final case object EmptyMonth extends FieldValidationError(Some(MonthField))

  final case object EmptyYear extends FieldValidationError(Some(YearField))

  final case object InvalidDay extends FieldValidationError(Some(DayField))

  final case object InvalidMonth extends FieldValidationError(Some(MonthField))

  final case object InvalidYear extends FieldValidationError(Some(YearField))

  final case object InvalidDate extends FieldValidationError(None)

  final case object InvalidYearLength extends FieldValidationError(Some(YearField))

  final case object TooEarly extends FieldValidationError(None)

  final case object TooLate extends FieldValidationError(None)

  private val allFields = "day-month-year"
  private val dateTooEarly = s"$allFields.min-date"
  private val dateTooLate = s"$allFields.max-date"
  private val emptyDate = s"$allFields.empty"

  /*
   * The regex expects the following message key format: s"$errorContext.$fieldKeys.error"
   *
   * Example:
   *   - agent.error.property.day-month-year.empty
   *   - agent.error.property.day-month.invalid
   *   - error.property.year.length
   */
  def highlightField(fieldKey: DateField, messageKey: String): Boolean =
    ("\\.[^.]*" + fieldKey + "[^.]*\\.[^.]*$").r
      .findFirstIn(messageKey)
      .nonEmpty

  def transformErrors(errors: List[FieldValidationError],
                      ids: HtmlIds,
                      isAgent: Boolean = false,
                      errorContext: String,
                      minDate: Option[LocalDate] = None,
                      maxDate: Option[LocalDate] = None,
                      dateFormatter: Option[LocalDate => String]): DateModelValidation =
    (collectInvalidErrors(errors), collectEmptyErrors(errors), collectLengthErrors(errors)) match {
      case (Nil, Nil, Nil) =>
        collectDateErrors(errors, ids, isAgent, errorContext, minDate, maxDate, dateFormatter)
      case (invalidFields, Nil, Nil) =>
        error(isAgent, errorContext, fieldsToFormKey(invalidFields, ids), s"${fieldsToMessageKey(invalidFields)}.invalid")
      case (Nil, emptyFields, Nil) =>
        error(isAgent, errorContext, fieldsToFormKey(emptyFields, ids), s"${fieldsToMessageKey(emptyFields)}.empty")
      case (Nil, Nil, invalidLengthFields) =>
        error(isAgent, errorContext, fieldsToFormKey(invalidLengthFields, ids), s"${fieldsToMessageKey(invalidLengthFields)}.length")
      case (invalidFields, emptyFields, invalidLengthFields) =>
        val fields = invalidFields ++ emptyFields ++ invalidLengthFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
  }

  private def fieldsToFormKey(fields: List[DateField], ids: HtmlIds) = {
    if (fields.contains(DayField)) ids.totalDayKey
    else if (fields.contains(MonthField)) ids.totalMonthKey
    else if (fields.contains(YearField)) ids.totalYearKey
    else ids.totalDayKey
  }

  private def fieldsToMessageKey(fields: List[DateField]): String =
    fields.distinct.sorted.mkString("-")

  private def collectInvalidErrors(errors: List[FieldValidationError]): List[DateField] = collectFields(errors, InvalidDay, InvalidMonth, InvalidYear)

  private def collectEmptyErrors(errors: List[FieldValidationError]): List[DateField] = collectFields(errors, EmptyDay, EmptyMonth, EmptyYear)

  private def collectLengthErrors(errors: List[FieldValidationError]): List[DateField] = collectFields(errors, InvalidYearLength)

  private def collectDateErrors(errors: List[FieldValidationError],
                                ids: HtmlIds,
                                isAgent: Boolean,
                                errorContext: String,
                                minDate: Option[LocalDate],
                                maxDate: Option[LocalDate],
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

  private def collectFields(errors: List[FieldValidationError], errorsToFind: FieldValidationError*): List[DateField] = {
    errorsToFind.foldLeft[List[DateField]](Nil)(
      (acc, errorToFind) => if (errors.contains(errorToFind)) acc ++ errorToFind.field else acc
    )
  }

  private def errorKey(isAgent: Boolean,
                       errorContext: String,
                       error: String): String =
    if (isAgent) s"agent.error.$errorContext.$error" else s"error.$errorContext.$error"

  private def error(isAgent: Boolean,
                    errorContext: String,
                    id: String, errorKeyName: String,
                    args: List[Any] = Nil) =
    Left(Seq(FormError(id, errorKey(isAgent, errorContext, errorKeyName), args)))

  private def defaultDateFormatter: LocalDate => String = d => d.toString
}
