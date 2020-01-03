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

package incometax.business.models

import core.models.DateModel
import incometax.util.AccountingPeriodUtil
import play.api.libs.json.Json


case class AccountingPeriodModel(startDate: DateModel, endDate: DateModel) {
  lazy val taxEndYear = AccountingPeriodUtil.getTaxEndYear(this)
  lazy val adjustedTaxYear =
    if (taxEndYear <= 2018) {
      val nextStartDate = this.endDate.toLocalDate.plusDays(1)
      val nextEndDate = nextStartDate.plusYears(1).minusDays(1)
      AccountingPeriodModel(DateModel.dateConvert(nextStartDate), DateModel.dateConvert(nextEndDate))
    }
    else this
}

object AccountingPeriodModel {
  implicit val format = Json.format[AccountingPeriodModel]
}
