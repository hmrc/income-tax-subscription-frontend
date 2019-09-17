/*
 * Copyright 2019 HM Revenue & Customs
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

package core.services

import java.time.LocalDate

import incometax.util.{AccountingPeriodUtil, CurrentDateProvider}
import javax.inject.{Inject, Singleton}

@Singleton
class AccountingPeriodService @Inject()(currentDateProvider: CurrentDateProvider) {
  def isEligibleAccountingPeriod(startDate: LocalDate, endDate: LocalDate): Boolean = {
    val isWholeYear = startDate.minusDays(1).plusYears(1) == endDate
    val currentTaxYearEndYear = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate())
    val isValidTaxYear = endDate.getYear >= currentTaxYearEndYear && endDate.getYear < currentTaxYearEndYear + 1

    isWholeYear && isValidTaxYear
  }
}