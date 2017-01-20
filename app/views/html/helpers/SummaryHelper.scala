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

package views.html.helpers

import forms.{IncomeSourceForm, IncomeTypeForm}
import models.{IncomeSourceModel, IncomeTypeModel}
import play.api.i18n.Messages

object SummaryHelper {

  def incomeTypeText(src: IncomeTypeModel)(implicit messages: Messages): String = src.incomeType match {
    case IncomeTypeForm.option_cash => Messages("business.income_type.cash")
    case IncomeTypeForm.option_accruals => Messages("business.income_type.accruals")
  }

  def incomeSourceText(src: IncomeSourceModel)(implicit messages: Messages): String = src.source match {
    case IncomeSourceForm.option_business => Messages("income_source.business")
    case IncomeSourceForm.option_property => Messages("income_source.property")
    case IncomeSourceForm.option_both => Messages("income_source.both")
  }

}
