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

package forms.validation.testutils

import forms.agent.{IncomeSourceForm, MatchTaxYearForm}
import forms.individual.business.BusinessNameForm._
import forms.individual.business.BusinessPhoneNumberForm._
import forms.individual.business.{AccountingMethodForm, AccountingYearForm}
import forms.submapping.DateMapping._
import forms.validation.utils.ConstraintUtil._
import play.api.data.validation._

object DataMap {

  object DataMap {

    type DataMap = Map[String, String]

    val EmptyMap: DataMap = DataMap()

    def DataMap(elems: (String, String)*): DataMap = Map(elems: _*)

    def date(prefix: String)(day: String, month: String, year: String): DataMap =
      Map(prefix * dateDay -> day, prefix * dateMonth -> month, prefix * dateYear -> year)

    val emptyDate: String => DataMap = (prefix: String) => date(prefix)("", "", "")

    def busName(name: String): DataMap = Map(businessName -> name)

    def busPhoneNumber(number: String): DataMap = Map(phoneNumber -> number)

    def matchTaxYear(iType: String): DataMap = Map(MatchTaxYearForm.matchTaxYear -> iType)

    def accountingMethod(iType: String): DataMap = Map(AccountingMethodForm.accountingMethod -> iType)

    def accountingYear(iType: String): DataMap = Map(AccountingYearForm.accountingYear -> iType)

    def incomeSource(iType: String): DataMap = Map(IncomeSourceForm.incomeSource -> iType)

    def individualIncomeSource(business: String, ukProperty: String): DataMap =
      Map(forms.individual.incomesource.IncomeSourceForm.business -> business,
        forms.individual.incomesource.IncomeSourceForm.ukProperty -> ukProperty)


    val alwaysFailInvalid: Invalid = Invalid("always fail")

    def alwaysFail[T]: Constraint[T] = constraint[T]((t: T) => alwaysFailInvalid)

  }

}
