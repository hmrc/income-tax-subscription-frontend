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

import models._
import models.common._
import models.common.business._
import models.usermatching.{UserDetailsModel, UserMatchSuccessResponseModel}
import uk.gov.hmrc.domain.Generator
import utilities.individual.TestConstants
import utilities.individual.TestConstants.{testFirstName, testLastName, testNino, testUtr}

import java.time.LocalDate

object TestModels {

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate = AccountingPeriodUtil.getCurrentTaxYearStartDate
  val testEndDate = AccountingPeriodUtil.getCurrentTaxYearEndDate

  val testAccountingPeriod: AccountingPeriodModel = testAccountingPeriod(testStartDate, testEndDate)
  val adjustedTestAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate.plusDays(1))

  val testAccountingPeriodMatched = AccountingPeriodModel(testStartDate, DateModel("05", "04", "2018"))

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testSelectedTaxYearCurrent = AccountingYearModel(Current, true)
  val testSelectedTaxYearNext = AccountingYearModel(Next)
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountMethod: AccountingMethod = Cash
  val testAccountingMethodAccrual = AccountingMethodModel(Accruals)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
  val testOverseasAccountingMethodProperty = OverseasAccountingMethodPropertyModel(Cash)
  val testBusinessTradeName = BusinessTradeNameModel("test trade name")
  val testBusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  val testBusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), Some("TF2 1PF")))
  val testId = "testId"

  val testValidStartDate = DateModel.dateConvert(LocalDate.now.minusYears(3))
  val testPropertyStartDateModel: PropertyStartDateModel = PropertyStartDateModel(testValidStartDate)
  val testOverseasPropertyStartDateModel: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testValidStartDate)

  val testFullPropertyModel: PropertyModel = PropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    startDate = Some(testPropertyStartDateModel.startDate),
    confirmed = true
  )

  val testFullOverseasPropertyModel: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    startDate = Some(testPropertyStartDateModel.startDate),
    confirmed = true
  )


  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, Some(testUtr))

  lazy val testMatchNoUtrModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, None)

  lazy val testSummaryDataSelfEmploymentData =
    Seq(SelfEmploymentData
    (
      id = testId,
      businessStartDate = Some(testBusinessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), Some("TF2 1PF"))))
    )
    )
}
