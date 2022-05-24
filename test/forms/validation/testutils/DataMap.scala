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

package forms.validation.testutils

import forms.agent.AccountingMethodOverseasPropertyForm
import forms.formatters.DateModelMapping
import forms.individual.business.AccountingYearForm
import forms.validation.utils.ConstraintUtil._
import play.api.data.validation._

object DataMap {

  object DataMap {

    type DataMap = Map[String, String]

    val EmptyMap: DataMap = DataMap()

    def DataMap(elems: (String, String)*): DataMap = Map(elems: _*)

    def date(prefix: String)(day: String, month: String, year: String): DataMap =
      Map(s"$prefix.${DateModelMapping.day}" -> day, s"$prefix.${DateModelMapping.month}" -> month, s"$prefix.${DateModelMapping.year}" -> year)

    def govukDate(prefix: String)(day: String, month: String, year: String): DataMap =
      Map(s"$prefix-${DateModelMapping.day}" -> day, s"$prefix-${DateModelMapping.month}" -> month, s"$prefix-${DateModelMapping.year}" -> year)

    val emptyDate: String => DataMap = (prefix: String) => date(prefix)("", "", "")

    def overseasPropertyAccountingMethod(iType: String): DataMap = Map(AccountingMethodOverseasPropertyForm.accountingMethodOverseasProperty -> iType)

    def accountingYear(iType: String): DataMap = Map(AccountingYearForm.accountingYear -> iType)

    def incomeSource(business: String, ukProperty: String, overseasProperty: String): DataMap =
      Map(forms.individual.incomesource.IncomeSourceForm.selfEmployedKey -> business,
        forms.individual.incomesource.IncomeSourceForm.ukPropertyKey -> ukProperty,
        forms.individual.incomesource.IncomeSourceForm.overseasPropertyKey -> overseasProperty)

    val alwaysFailInvalid: Invalid = Invalid("always fail")

    def alwaysFail[T]: Constraint[T] = constraint[T]((t: T) => alwaysFailInvalid)

  }

}
