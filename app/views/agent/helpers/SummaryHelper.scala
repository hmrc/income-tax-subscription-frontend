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

package views.agent.helpers

import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, IncomeSourceModel}
import models.{Accruals, Cash, Current, Next}
import play.api.i18n.Messages
import utilities.AccountingPeriodUtil.getCurrentTaxEndYear

object SummaryHelper {

  def accountingMethodText(src: AccountingMethodModel)(implicit messages: Messages): String = src.accountingMethod match {
    case Cash => Messages("agent.summary.income_type.cash")
    case Accruals => Messages("agent.summary.income_type.accruals")
  }

  def accountingMethodText(src: AccountingMethodPropertyModel)(implicit messages: Messages): String = src.propertyAccountingMethod match {
    case Cash => Messages("agent.summary.income_type.cash")
    case Accruals => Messages("agent.summary.income_type.accruals")
  }

  def incomeSourceText(src: IncomeSourceModel)(implicit messages: Messages): String = src match {
    case IncomeSourceModel(true, false, _) => Messages("agent.summary.income_source.business")
    case IncomeSourceModel(false, true, _) => Messages("agent.summary.income_source.property")
    case IncomeSourceModel(true, true, _) => Messages("agent.summary.income_source.both")
  }

  def accountingYearText(src: AccountingYearModel)(implicit messages: Messages): String = src.accountingYear match {
    case Current => Messages("agent.summary.selected_year.current", (getCurrentTaxEndYear - 1).toString, getCurrentTaxEndYear.toString)
    case Next => Messages("agent.summary.selected_year.next", getCurrentTaxEndYear.toString, (getCurrentTaxEndYear + 1).toString)
  }
}
