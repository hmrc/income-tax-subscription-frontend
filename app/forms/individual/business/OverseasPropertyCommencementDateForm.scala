/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.individual.business

import java.time.LocalDate

import forms.submapping.DateMapping._
import forms.validation.utils.ConstraintUtil._
import models.DateModel
import models.individual.business.OverseasPropertyCommencementDateModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationResult}

import scala.util.Try

object OverseasPropertyCommencementDateForm {


  def overseasPropertyStartDate: LocalDate = LocalDate.now().minusYears(1)

  val startDate: String = "startDate"

  val dateValidation: Constraint[(Option[String], Option[String], Option[String])] = constraint[(Option[String], Option[String], Option[String])] {
    case (day, month, year) => {
      lazy val invalidDate = Invalid("foreign.property.error.date.empty")
      Try[ValidationResult] {
        LocalDate.of(year.get.toInt, month.get.toInt, day.get.toInt)
        Valid
      }.getOrElse(invalidDate)
    }
  }

  val validateDate: Constraint[(Option[String], Option[String], Option[String])] = constraint[(Option[String], Option[String], Option[String])] {
    case (None, None, None) => Invalid("foreign.property.error.date.empty")
    case (Some(_), None, None) => Invalid("foreign.property.error.month.year.empty")
    case (None, None, Some(_)) => Invalid("foreign.property.error.day.month.empty")
    case (None, Some(_), None) => Invalid("foreign.property.error.day.year.empty")
    case (Some(_), Some(_), None) => Invalid("foreign.property.error.year.empty")
    case (None, Some(_), Some(_)) => Invalid("foreign.property.error.day.empty")
    case (Some(_), None, Some(_)) => Invalid("foreign.property.error.month.empty")
    case (Some(_), Some(_), Some(_)) => Valid


  }

  private val toDateModel: (Option[String], Option[String], Option[String]) => DateModel = {
    case (day, month, year) => DateModel(day.get, month.get, year.get)
  }

  private val fromDateModel: DateModel => (Option[String], Option[String], Option[String]) = {
    dateModel => (Some(dateModel.day), Option(dateModel.month), Option(dateModel.year))
  }


  def startBeforeOneYear(date: String): Constraint[DateModel] = constraint[DateModel](
    dateModel => {
      lazy val invalid = Invalid("foreign.property.error.property_commencement_date.minStartDate", date)
      if (DateModel.dateConvert(dateModel).isAfter(overseasPropertyStartDate)) invalid else Valid
    }
  )

  def overseasPropertyCommencementDateForm(date: String): Form[OverseasPropertyCommencementDateModel] = Form(
    mapping(
      startDate -> optDateMapping.verifying(validateDate andThen dateValidation).
        transform[DateModel](toDateModel.tupled, fromDateModel).verifying(startBeforeOneYear(date))
    )(OverseasPropertyCommencementDateModel.apply)(OverseasPropertyCommencementDateModel.unapply)
  )



}
