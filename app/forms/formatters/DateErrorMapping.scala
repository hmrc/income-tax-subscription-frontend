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
import play.api.data.FormError

import java.time.LocalDate

object DateErrorMapping {
  final case class InvalidFields(fields: List[String])
  final case class EmptyFields(fields: List[String])
  final case class WrongLengthFields(fields: List[String])

  def transformErrors(errors: List[String],
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
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.length")
      }
      case (None, Some(EmptyFields(fields)), None) => {
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.empty")
      }
      case (None, Some(EmptyFields(emptyFields)), Some(WrongLengthFields(wrongLengthFields))) => {
        val fields = emptyFields ++ wrongLengthFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
      }
      case (Some(InvalidFields(fields)), None, None) => {
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
      }
      case (Some(InvalidFields(invalidFields)), None, Some(WrongLengthFields(wrongLengthFields))) => {
        val fields = invalidFields ++ wrongLengthFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
      }
      case (Some(InvalidFields(invalidFields)), Some(EmptyFields(emptyFields)), None) => {
        val fields = invalidFields ++ emptyFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(invalidFields ++ emptyFields)}.invalid")
      }
      case (Some(InvalidFields(invalidFields)), Some(EmptyFields(emptyFields)), Some(WrongLengthFields(wrongLengthFields))) => {
        val fields = invalidFields ++ emptyFields ++ wrongLengthFields
        error(isAgent, errorContext, fieldsToFormKey(fields, ids), s"${fieldsToMessageKey(fields)}.invalid")
      }
    }
  }

  private def fieldsToFormKey(fields: List[String], ids: HtmlIds) = {
    (fields.contains("day"), fields.contains("month"), fields.contains("year")) match {
      case (true, _, _) => ids.totalDayKey
      case (false, true, _) => ids.totalMonthKey
      case (false, false, true) => ids.totalYearKey
      case _ => ids.totalDayKey
    }
  }

  private def fieldsToMessageKey(fields: List[String]): String =
    fields.toSet.toList.sorted.mkString("_")

  private def mapToInvalidError(errors: List[String]): Option[InvalidFields] = {
    val fields = collectFields(errors.contains("day.invalid"), errors.contains("month.invalid"), errors.contains("year.invalid"))
    if(fields.isEmpty) None else Some(InvalidFields(fields))
  }

  private def mapToEmptyError(errors: List[String]): Option[EmptyFields] = {
    val fields = collectFields(errors.contains("day.empty"), errors.contains("month.empty"), errors.contains("year.empty"))
    if(fields.isEmpty) None else Some(EmptyFields(fields))
  }

  private def mapToYearError(errors: List[String]): Option[WrongLengthFields] = {
    if(errors.contains("year.length")) {
      Some(WrongLengthFields(List("year")))
    } else {
      None
    }
  }

  private def mapToDateError(errors: List[String],
                             ids: HtmlIds,
                             isAgent: Boolean = false,
                             errorContext: String,
                             minDate: Option[LocalDate] = None,
                             maxDate: Option[LocalDate] = None,
                             dateFormatter: Option[LocalDate => String]): DateModelValidation = {
    val formatter: LocalDate => String = dateFormatter.getOrElse(defaultDateFormatter)

    if(errors.contains("date.minDate")) {
      error(
        isAgent,
        errorContext,
        ids.totalDayKey,
        "day_month_year.minDate",
        minDate.fold(List.empty[String])(value => List(formatter(value)))
      )
    } else if(errors.contains("date.maxDate")) {
      error(
        isAgent,
        errorContext,
        ids.totalDayKey,
        "day_month_year.maxDate",
        maxDate.fold(List.empty[String])(value => List(formatter(value)))
      )
    } else {
      error(
        isAgent,
        errorContext,
        ids.totalDayKey,
        "day_month_year.empty"
      )
    }
  }

  private def collectFields(containsDay: Boolean, containsMonth: Boolean, containsYear: Boolean): List[String] = {
    (containsDay, containsMonth, containsYear) match {
      case (false, false, true) => List("year")
      case (false, true, false) => List("month")
      case (false, true, true) => List("month", "year")
      case (true, false, false) => List("day")
      case (true, false, true) => List("day", "year")
      case (true, true, false) => List("day", "month")
      case (true, true, true) => List("day", "month", "year")
      case _ => List.empty[String]
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
