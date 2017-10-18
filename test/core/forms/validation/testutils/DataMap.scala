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

package core.forms.validation.testutils

import incometax.incomesource.forms.IncomeSourceForm
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil.constraint
import incometax.business.forms.AccountingPeriodPriorForm
import play.api.data.validation.{Constraint, Invalid}

object DataMap {

  import incometax.business.forms.BusinessNameForm._
  import incometax.business.forms.BusinessPhoneNumberForm._
  import forms.NotEligibleForm._
  import core.forms.submapping.DateMapping._
  import incometax.business.forms.AccountingMethodForm


  type DataMap = Map[String, String]

  val EmptyMap = DataMap()

  def DataMap(elems: (String, String)*): DataMap = Map(elems: _*)

  def date(prefix: String)(day: String, month: String, year: String): DataMap =
    Map(prefix * dateDay -> day, prefix * dateMonth -> month, prefix * dateYear -> year)

  val emptyDate: String => DataMap = (prefix: String) => date(prefix)("", "", "")

  def busName(name: String): DataMap = Map(businessName -> name)

  def busPhoneNumber(number: String): DataMap = Map(phoneNumber -> number)

  def accountingPeriodPrior(currentPeriodIsPrior: String): DataMap = Map(AccountingPeriodPriorForm.accountingPeriodPrior -> currentPeriodIsPrior)

  def accountingMethod(iType: String): DataMap = Map(AccountingMethodForm.accountingMethod -> iType)

  def incomeSource(iType: String): DataMap = Map(IncomeSourceForm.incomeSource -> iType)

  def notEligibleChoice(iType: String): DataMap = Map(choice -> iType)

  val alwaysFailInvalid: Invalid = ErrorMessageFactory.error("always fail")

  def alwaysFail[T]: Constraint[T] = constraint[T]((t: T) => alwaysFailInvalid)

}
