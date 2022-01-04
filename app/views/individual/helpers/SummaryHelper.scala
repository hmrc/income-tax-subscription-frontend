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

package views.individual.helpers

import models.common.{AccountingYearModel, IncomeSourceModel}
import models.{AccountingMethod, Accruals, Cash, Current, Next}
import play.api.i18n.Messages
import utilities.AccountingPeriodUtil.getCurrentTaxEndYear

object SummaryHelper {

  def accountingMethodText(accountingMethod: AccountingMethod)(implicit messages: Messages): String = accountingMethod match {
    case Cash => Messages("summary.income_type.cash")
    case Accruals => Messages("summary.income_type.accruals")
  }

  def accountingYearText(src: AccountingYearModel)(implicit messages: Messages): String = src.accountingYear match {
    case Current => Messages("summary.selected_year.current", (getCurrentTaxEndYear -1).toString , getCurrentTaxEndYear.toString)
    case Next => Messages("summary.selected_year.next", getCurrentTaxEndYear.toString, (getCurrentTaxEndYear + 1).toString)
  }

  def incomeSourceText(src: IncomeSourceModel)(implicit messages: Messages): String = {
    def messageOrNone(message: String, condition: Boolean): Option[String] = {
      if (condition) Some(Messages(message)) else None
    }
    Seq(messageOrNone(Messages("summary.income_source.business"), src.selfEmployment),
      messageOrNone(Messages("summary.income_source.property"), src.ukProperty),
      messageOrNone(Messages("summary.income_source.foreign_property"), src.foreignProperty)
    ).flatten.mkString("<br>")
  }
}
