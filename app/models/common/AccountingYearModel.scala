/*
 * Copyright 2023 HM Revenue & Customs
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

package models.common

import models.{AccountingYear, Current, Next}
import play.api.libs.json.{Json, OFormat}
import utilities.AccountingPeriodUtil.{getCurrentTaxEndYear, getCurrentTaxStartYear, getNextTaxEndYear, getNextTaxStartYear}

case class AccountingYearModel(accountingYear: AccountingYear, confirmed: Boolean = false, editable: Boolean = true) {
  lazy val toFullYearFormat: String = accountingYear match {
    case Current => s"$getCurrentTaxStartYear-$getCurrentTaxEndYear"
    case Next => s"$getNextTaxStartYear-$getNextTaxEndYear"
  }
}

object AccountingYearModel {
  implicit val format: OFormat[AccountingYearModel] = Json.using[Json.WithDefaultValues].format[AccountingYearModel]
}
