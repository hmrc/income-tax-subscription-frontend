/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.submapping.AccountingMethodMapping
import forms.submapping.AccountingMethodMapping.{option_accruals, option_cash}
import models.common.OverseasPropertyModel
import models.{AccountingMethod, Accruals, Cash, DateModel}
import play.api.data.{Form, Mapping}
import play.api.data.Forms.tuple
import play.api.data.validation.Invalid

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IncomeSourcesOverseasPropertyForm {

  val startDate: String = "startDate"
  val accountingMethodOverseasProperty: String = "accountingMethodOverseasProperty"
  val errorContext: String = "overseas.property"
  def maxStartDate: LocalDate = LocalDate.now().plusDays(6)
  def minStartDate: LocalDate = LocalDate.of(1900, 1, 1)

  def overseasPropertyStartDate(f: LocalDate => String): Mapping[DateModel] = dateModelMapping(
    isAgent = true,
    errorContext = errorContext,
    minDate = Some(minStartDate),
    maxDate = Some(maxStartDate),
    Some(f)
  )

  val overseasPropertyAccountingMethod: Mapping[AccountingMethod] = AccountingMethodMapping(
    errInvalid = Invalid("agent.error.accounting-method-property.invalid"),
    errEmpty = Some(Invalid("agent.error.accounting-method-property.invalid"))
  )

  def incomeSourcesOverseasPropertyForm(f: LocalDate => String): Form[(DateModel, AccountingMethod)] = Form(
    tuple(
      startDate -> overseasPropertyStartDate(f),
      accountingMethodOverseasProperty -> overseasPropertyAccountingMethod
    )
  )

  def createOverseasPropertyMapData(overseasProperty: Option[OverseasPropertyModel]): Map[String, String] = {

    val dateMap: Map[String, String] = overseasProperty.flatMap(_.startDate).fold(Map.empty[String, String]) { date =>
      Map(
        s"$startDate-dateDay" -> date.day,
        s"$startDate-dateMonth" -> date.month,
        s"$startDate-dateYear" -> date.year
      )
    }

    val accountingMethodMap: Map[String, String] = overseasProperty.flatMap(_.accountingMethod).fold(Map.empty[String, String]) {
      case Cash => Map(accountingMethodOverseasProperty -> option_cash)
      case Accruals => Map(accountingMethodOverseasProperty -> option_accruals)
    }

    dateMap ++ accountingMethodMap

  }
}
