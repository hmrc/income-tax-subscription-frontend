/*
 * Copyright 2017 HM Revenue & Customs
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

package core.views.html

import incometax.business.forms.AccountingMethodForm
import incometax.business.models.AccountingMethodModel
import incometax.incomesource.forms.IncomeSourceForm
import incometax.incomesource.models.IncomeSourceModel
import play.api.i18n.Messages

object SummaryHelper {

  def accountingMethodText(src: AccountingMethodModel)(implicit messages: Messages): String = src.accountingMethod match {
    case AccountingMethodForm.option_cash => Messages("summary.income_type.cash")
    case AccountingMethodForm.option_accruals => Messages("summary.income_type.accruals")
  }

  def incomeSourceText(src: IncomeSourceModel)(implicit messages: Messages): String = src.source match {
    case IncomeSourceForm.option_business => Messages("summary.income_source.business")
    case IncomeSourceForm.option_property => Messages("summary.income_source.property")
    case IncomeSourceForm.option_both => Messages("summary.income_source.both")
  }

}
