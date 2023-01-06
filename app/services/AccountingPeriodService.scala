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

package services

import models._
import models.common.AccountingPeriodModel
import utilities.AccountingPeriodUtil._
import utilities.{AccountingPeriodUtil, CurrentDateProvider}

import java.time.LocalDate
import java.time.Month._
import javax.inject.{Inject, Singleton}
import scala.Ordering.Implicits._

@Singleton
class AccountingPeriodService @Inject()(currentDateProvider: CurrentDateProvider) {

  val FIFTH: Int = 5
  val SIXTH: Int = 6

  def checkEligibleAccountingPeriod(startDate: LocalDate, endDate: LocalDate, hasPropertyType: Boolean): Boolean = {
    val taxYear = AccountingPeriodUtil.getTaxEndYear(endDate)

    val isEligibleTaxYear = {
      if (hasPropertyType) taxYear == currentTaxYear
      else taxYear == currentTaxYear || taxYear == currentTaxYear + 1
    }

    val isEligibleStartDate = if (endDate.getDayOfMonth == 5 && endDate.getMonth == APRIL) {
      val minimumStartDate = LocalDate.of(endDate.getYear - 1, APRIL, 1)
      val maximumStartDate = LocalDate.of(endDate.getYear - 1, APRIL, 6)
      startDate >= minimumStartDate && startDate <= maximumStartDate
    } else startDate == endDate.minusYears(1).plusDays(1)

    isEligibleTaxYear && isEligibleStartDate
  }

  def currentTaxYear: Int = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate)

  object AgentUpdateDates {
    val currentDate: LocalDate = currentDateProvider.getCurrentDate
    val taxYearEnd: Int = getTaxEndYear(currentDate)
    val taxYearQ1: LocalDate = LocalDate.of(taxYearEnd - 1, JULY, 5)
    val taxYearQ2: LocalDate = LocalDate.of(taxYearEnd - 1, OCTOBER, 5)
    val taxYearQ3: LocalDate = LocalDate.of(taxYearEnd, JANUARY, 5)
    val taxYearQ4: LocalDate = LocalDate.of(taxYearEnd, APRIL, 5)
  }

  def getAllUpdateAndDeadlineDates(selectedTaxYear: AccountingYear): List[UpdateDeadline] = {
    val taxYear: Int = selectedTaxYear match {
      case Current => currentTaxYear
      case Next => currentTaxYear + 1
    }
    List(
      UpdateDeadline(
        AccountingPeriodModel(
          updateFrom = LocalDate.of(taxYear - 1, APRIL, SIXTH),
          updateTo = LocalDate.of(taxYear - 1, JULY, FIFTH),
        ),
        deadline = LocalDate.of(taxYear - 1, AUGUST, FIFTH)
      ),
      UpdateDeadline(AccountingPeriodModel(
        updateFrom = LocalDate.of(taxYear - 1, JULY, SIXTH),
        updateTo = LocalDate.of(taxYear - 1, OCTOBER, FIFTH)
      ),
        deadline = LocalDate.of(taxYear - 1, NOVEMBER, FIFTH)
      ),
      UpdateDeadline(
        AccountingPeriodModel(
          updateFrom = LocalDate.of(taxYear - 1, OCTOBER, SIXTH),
          updateTo = LocalDate.of(taxYear, JANUARY, FIFTH)
        ),
        deadline = LocalDate.of(taxYear, FEBRUARY, FIFTH)
      ),
      UpdateDeadline(
        AccountingPeriodModel(
          updateFrom = LocalDate.of(taxYear, JANUARY, SIXTH),
          updateTo = LocalDate.of(taxYear, APRIL, FIFTH)
        ),
        deadline = LocalDate.of(taxYear, MAY, FIFTH)
      )
    )
  }

  def getCurrentYearUpdateDates: UpdateDeadlineDates = {
    val allUpdateAndDeadlineDates: List[UpdateDeadline] = getAllUpdateAndDeadlineDates(Current)
    UpdateDeadlineDates(
      previous = allUpdateAndDeadlineDates.filter(_.accountingPeriodModel.endDate.toLocalDate <= AgentUpdateDates.currentDate),
      next = allUpdateAndDeadlineDates.filter(_.accountingPeriodModel.endDate.toLocalDate > AgentUpdateDates.currentDate)
    )
  }

}
