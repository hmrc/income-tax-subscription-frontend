/*
 * Copyright 2021 HM Revenue & Customs
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

import utilities.AccountingPeriodUtil._
import utilities.{AccountingPeriodUtil, CurrentDateProvider}

import java.time.LocalDate
import java.time.Month._
import javax.inject.{Inject, Singleton}
import scala.Ordering.Implicits._

@Singleton
class AccountingPeriodService @Inject()(currentDateProvider: CurrentDateProvider) {

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
    val updateDates = List((taxYearQ1, "agent.sign-up.complete.julyUpdate", (currentTaxYear - 1).toString),
      (taxYearQ2, "agent.sign-up.complete.octoberUpdate", (currentTaxYear - 1).toString),
      (taxYearQ3, "agent.sign-up.complete.januaryUpdate", currentTaxYear.toString),
      (taxYearQ4, "agent.sign-up.complete.aprilUpdate", currentTaxYear.toString))
  }

  def updateDatesBefore(): List[(String, String)] = {
    AgentUpdateDates.updateDates.filter(x => x._1 <= AgentUpdateDates.currentDate).map(x => (x._2, x._3))
  }

  def updateDatesAfter(): List[(String, String)] = {
    AgentUpdateDates.updateDates.filter(x => x._1 > AgentUpdateDates.currentDate).map(x => (x._2, x._3))
  }

}
