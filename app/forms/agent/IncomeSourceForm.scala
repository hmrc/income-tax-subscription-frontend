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

import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.data.Forms._

object IncomeSourceForm {

  val incomeSourceKey: String = "IncomeSource"

  val selfEmployedKey = "SelfEmployed"
  val ukPropertyKey = "UkProperty"
  val overseasPropertyKey = "OverseasProperty"

  def isAnyCheckboxSelected(list: List[String]): Boolean = {
    list.contains(selfEmployedKey) || list.contains(ukPropertyKey) || list.contains(overseasPropertyKey)
  }

  def toIncomeSourceModel(list: List[String]): IncomeSourceModel = {
    IncomeSourceModel(list.contains(selfEmployedKey), list.contains(ukPropertyKey), list.contains(overseasPropertyKey))
  }

  def fromIncomeSourceModel(model: IncomeSourceModel): List[String] = {
    List(
      if (model.selfEmployment) Some(selfEmployedKey) else None,
      if (model.ukProperty) Some(ukPropertyKey) else None,
      if (model.foreignProperty) Some(overseasPropertyKey) else None
    ).flatten
  }

  private def errorKey(overseasPropertyEnabled: Boolean): String = {
    if (overseasPropertyEnabled) {
      "agent.error.income_source_foreignProperty.invalid"
    } else {
      "agent.error.income_source.invalid"
    }
  }

  def incomeSourceForm(overseasPropertyEnabled: Boolean): Form[IncomeSourceModel] = {
    Form(
      single(
        incomeSourceKey -> list(text)
          .verifying(errorKey(overseasPropertyEnabled), isAnyCheckboxSelected _)
          .transform[IncomeSourceModel](toIncomeSourceModel, fromIncomeSourceModel)
      )
    )
  }


}
