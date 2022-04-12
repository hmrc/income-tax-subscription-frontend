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

package forms.agent

import forms.formatters.DateModelMapping.dateModelMapping
import models.DateModel
import play.api.data.Form
import play.api.data.Forms.single

import java.time.LocalDate

object OverseasPropertyStartDateForm {
  val startDate: String = "startDate"

  def maxStartDate: LocalDate = LocalDate.now().minusYears(1)

  def minStartDate: LocalDate = LocalDate.of(1900, 1, 1)

  val errorContext: String = "overseas.property"

  def overseasPropertyStartDateForm(minStartDate: LocalDate, maxStartDate: LocalDate, f: LocalDate => String): Form[DateModel] = Form(
    single(
      startDate -> dateModelMapping(isAgent = true, errorContext = errorContext, Some(minStartDate), Some(maxStartDate), Some(f))
    )
  )
}
