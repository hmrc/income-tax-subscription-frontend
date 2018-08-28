/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.views.html.helpers


import core.models.{Accruals, Cash}
import incometax.business.models.AccountingMethodModel
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import play.api.i18n.Messages

object SummaryHelper {

  def accountingMethodText(src: AccountingMethodModel)(implicit messages: Messages): String = src.accountingMethod match {
    case Cash => Messages("agent.summary.income_type.cash")
    case Accruals => Messages("agent.summary.income_type.accruals")
  }

  def incomeSourceText(src: IncomeSourceType)(implicit messages: Messages): String = src match {
    case Business => Messages("agent.summary.income_source.business")
    case Property => Messages("agent.summary.income_source.property")
    case Both => Messages("agent.summary.income_source.both")
  }

}
