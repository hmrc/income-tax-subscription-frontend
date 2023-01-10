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

package utilities

import java.time.LocalDate

import models.DateModel
import models.common.AccountingPeriodModel
import java.time.Month.JANUARY
import java.time.Month.APRIL

object AccountingPeriodUtil {
  private val fifth = 5
  private val sixth = 6
  private val thirtyFirst = 31

  def getTaxEndYear(date: LocalDate): Int =
    if (date.isBefore(LocalDate.of(date.getYear, APRIL.getValue, sixth))) date.getYear
    else date.getYear + 1

  def getTaxEndYear(accountingPeriodModel: AccountingPeriodModel): Int = getTaxEndYear(accountingPeriodModel.endDate.toLocalDate)

  def getCurrentTaxEndYear: Int = getTaxEndYear(LocalDate.now())

  def getEndOfPeriodStatementDate(isNextTaxYear: Boolean): LocalDate = {
    val year = if (isNextTaxYear) getCurrentTaxEndYear + 1 else getCurrentTaxEndYear
    LocalDate.of(year, JANUARY, thirtyFirst)
  }

  def getFinalDeclarationDate(isNextTaxYear: Boolean): LocalDate = {
    val year = if (isNextTaxYear) getCurrentTaxEndYear + 1 else getCurrentTaxEndYear
    LocalDate.of(year, JANUARY, thirtyFirst)
  }

  def getCurrentTaxYearStartDate: DateModel = DateModel(sixth.toString, APRIL.getValue.toString, (getCurrentTaxEndYear - 1).toString)

  def getCurrentTaxYearEndDate: DateModel = DateModel(fifth.toString, APRIL.getValue.toString, getCurrentTaxEndYear.toString)

  def getCurrentTaxYear: AccountingPeriodModel = AccountingPeriodModel(getCurrentTaxYearStartDate, getCurrentTaxYearEndDate)

  def getNextTaxYear: AccountingPeriodModel = AccountingPeriodModel(getCurrentTaxYearStartDate.plusYears(1), getCurrentTaxYearEndDate.plusYears(1))

  def getTaxYear(next: Boolean): AccountingPeriodModel = if (next) getNextTaxYear else getCurrentTaxYear

  implicit object LocalDateOrdering extends Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x.compareTo(y)
  }

}
