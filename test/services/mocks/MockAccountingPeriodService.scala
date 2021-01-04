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

package services.mocks

import java.time.LocalDate

import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.AccountingPeriodService

trait MockAccountingPeriodService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAccountingPeriodService)
  }

  val mockAccountingPeriodService: AccountingPeriodService = mock[AccountingPeriodService]

  def mockCheckEligibleAccountingPeriod(startDate: LocalDate, endDate: LocalDate, hasPropertyIncomeSource: Boolean)(eligible: Boolean): Unit =
    when(mockAccountingPeriodService.checkEligibleAccountingPeriod(startDate, endDate, hasPropertyIncomeSource)).thenReturn(eligible)

  def mockUpdateDateBefore(eligible: List[(String, String)]): Unit =
    when(mockAccountingPeriodService.updateDatesBefore).thenReturn(eligible)

  def mockUpdateDateAfter(eligible: List[(String, String)]): Unit =
    when(mockAccountingPeriodService.updateDatesAfter).thenReturn(eligible)
}
