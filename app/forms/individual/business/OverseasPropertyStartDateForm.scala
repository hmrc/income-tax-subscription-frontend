/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.formatters.DateModelMapping.dateModelMapping
import forms.validation.utils.ConstraintUtil._
import models.DateModel
import models.common.OverseasPropertyStartDateModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate

object OverseasPropertyStartDateForm {

  val startDate: String = "startDate"

  val maxStartDate: LocalDate = LocalDate.now().minusYears(1)

  val minStartDate: LocalDate = LocalDate.of(1900, 1, 1)

  val errorContext: String = "overseas.property"

  def earliestTaxYear(date: String): Constraint[DateModel] = constraint[DateModel] { dateModel =>
    val earliestAllowedYear: Int = minStartDate.getYear
    if (dateModel.year.toInt < earliestAllowedYear) {
      Invalid(s"error.$errorContext.start_date.minStartDate", date)
    } else {
      Valid
    }
  }

  def startBeforeOneYear(date: String): Constraint[DateModel] = constraint[DateModel] { dateModel =>
    if (DateModel.dateConvert(dateModel).isAfter(maxStartDate)) {
      Invalid(s"error.$errorContext.start_date.maxStartDate", date)
    } else {
      Valid
    }
  }

  def overseasPropertyStartDateForm(minStartDate: String, maxStartDate: String): Form[OverseasPropertyStartDateModel] = Form(
    mapping(
      startDate -> dateModelMapping(errorContext = errorContext).verifying(
        startBeforeOneYear(maxStartDate) andThen earliestTaxYear(minStartDate)
      )
    )(OverseasPropertyStartDateModel.apply)(OverseasPropertyStartDateModel.unapply)
  )


}
