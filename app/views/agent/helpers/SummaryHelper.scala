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

import models.common._
import models._
import play.api.i18n.Messages
import utilities.AccountingPeriodUtil.getCurrentTaxEndYear

object SummaryHelper {

  def accountingMethodText(accountingMethod: AccountingMethod)(implicit messages: Messages): String = accountingMethod match {
    case Cash => messages("summary.income_type.cash")
    case Accruals => messages("summary.income_type.accruals")
  }

  def incomeSourceText(src: IncomeSourceModel)(implicit messages: Messages): String = {
    def messageOrNone(message: String, condition: Boolean): Option[String] = {
      if (condition) Some(messages(message)) else None
    }

    Seq(messageOrNone(messages("agent.summary.income_source.business"), src.selfEmployment),
      messageOrNone(messages("agent.summary.income_source.uk_property"), src.ukProperty),
      messageOrNone(messages("agent.summary.income_source.overseas_property"), src.foreignProperty)
    ).flatten.mkString("<br>")
  }

  def accountingYearText(src: AccountingYearModel)(implicit messages: Messages): String = src.accountingYear match {
    case Current => messages("agent.summary.selected_year.current", (getCurrentTaxEndYear - 1).toString, getCurrentTaxEndYear.toString)
    case Next => messages("agent.summary.selected_year.next", getCurrentTaxEndYear.toString, (getCurrentTaxEndYear + 1).toString)
  }
}
