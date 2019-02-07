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

package incometax.util

import java.time.LocalDate

import core.models.DateModel
import incometax.business.models.AccountingPeriodModel


object AccountingPeriodUtil {
  private val april = 4
  private val fifth = 5
  private val sixth = 6

  def getTaxEndYear(date: LocalDate): Int =
    if (date.isBefore(LocalDate.of(date.getYear, april, sixth))) date.getYear
    else date.getYear + 1

  def getTaxEndYear(accountingPeriodModel: AccountingPeriodModel): Int = getTaxEndYear(accountingPeriodModel.endDate.toLocalDate)

  def getCurrentTaxEndYear: Int = getTaxEndYear(LocalDate.now())

  def getCurrentTaxYearStartDate = DateModel(sixth.toString, april.toString, (getCurrentTaxEndYear -1).toString)

  def getCurrentTaxYearEndDate = DateModel(fifth.toString, april.toString, getCurrentTaxEndYear.toString)

  def getCurrentTaxYear = AccountingPeriodModel(getCurrentTaxYearStartDate, getCurrentTaxYearEndDate)

}
