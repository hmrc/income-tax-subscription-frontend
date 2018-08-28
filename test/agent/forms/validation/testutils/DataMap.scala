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

package agent.forms.validation.testutils

import agent.forms.AccountingPeriodPriorForm
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil.constraint
import play.api.data.validation.{Constraint, Invalid}

object DataMap {

  import agent.forms.BusinessNameForm._
  import agent.forms.OtherIncomeForm._
  import agent.forms.{IncomeSourceForm, AccountingMethodForm}
  import agent.forms.preferences.BackToPreferencesForm
  import core.forms.submapping.DateMapping._


  type DataMap = Map[String, String]

  val EmptyMap = DataMap()

  def DataMap(elems: (String, String)*): DataMap = Map(elems: _*)

  def date(prefix: String)(day: String, month: String, year: String): DataMap =
    Map(prefix * dateDay -> day, prefix * dateMonth -> month, prefix * dateYear -> year)

  val emptyDate: String => DataMap = (prefix: String) => date(prefix)("", "", "")

  def busName(name: String): DataMap = Map(businessName -> name)

  def accountingPeriodPrior(currentPeriodIsPrior: String): DataMap = Map(AccountingPeriodPriorForm.accountingPeriodPrior -> currentPeriodIsPrior)

  def accountingMethod(iType: String): DataMap = Map(AccountingMethodForm.accountingMethod -> iType)

  def incomeSource(iType: String): DataMap = Map(IncomeSourceForm.incomeSource -> iType)

  def otherIncomeChoice(iType: String): DataMap = Map(choice -> iType)

  def acceptPaperlessDataMap(accept: String): DataMap = Map(BackToPreferencesForm.backToPreferences -> accept)

  val alwaysFailInvalid: Invalid = ErrorMessageFactory.error("always fail")

  def alwaysFail[T]: Constraint[T] = constraint[T]((t: T) => alwaysFailInvalid)

}
