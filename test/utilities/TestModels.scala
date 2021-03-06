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

package utilities

import java.time.LocalDate

import models._
import models.common.{IncomeSourceModel, _}
import models.common.business._
import models.usermatching.{UserDetailsModel, UserMatchSuccessResponseModel}
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.individual.TestConstants
import utilities.individual.TestConstants.{testFirstName, testLastName, testNino, testUtr}

object TestModels extends Implicits {

  import SubscriptionDataKeys._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate = AccountingPeriodUtil.getCurrentTaxYearStartDate
  val testEndDate = AccountingPeriodUtil.getCurrentTaxYearEndDate

  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)
  val adjustedTestAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate.plusDays(1))

  val testAccountingPeriodMatched = AccountingPeriodModel(testStartDate, DateModel("05", "04", "2018"))

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testSelectedTaxYearCurrent = AccountingYearModel(Current)
  val testSelectedTaxYearNext = AccountingYearModel(Next)
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodAccrual = AccountingMethodModel(Accruals)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
  val testOverseasAccountingMethodProperty = OverseasAccountingMethodPropertyModel(Cash)
  val testBusinessTradeName = BusinessTradeNameModel("test trade name")
  val testBusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  val testBusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF"))
  val testId = "testId"

  val testValidStartDate = DateModel.dateConvert(LocalDate.now.minusYears(3))
  val testPropertyStartDateModel: PropertyStartDateModel = PropertyStartDateModel(testValidStartDate)
  val testOverseasPropertyStartDateModel: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testValidStartDate)

  val emptyCacheMap = CacheMap("", Map())

  lazy val testCacheMap: CacheMap =
    testCacheMap(
      incomeSource = testAgentIncomeSourceBusinessProperty,
      businessName = testBusinessName,
      selectedTaxYear = testSelectedTaxYearNext,
      accountingMethod = testAccountingMethod,
      accountingMethodProperty = testAccountingMethodProperty
    )

  def testCacheMapCustom(incomeSource: Option[IncomeSourceModel] = testAgentIncomeSourceBusinessProperty,
                         businessName: Option[BusinessNameModel] = testBusinessName,
                         selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYearNext,
                         accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                         accountingMethodProperty: Option[AccountingMethodPropertyModel] = testAccountingMethodProperty,
                         overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = testOverseasAccountingMethodProperty): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      businessName = businessName,
      selectedTaxYear = selectedTaxYear,
      accountingMethod = accountingMethod,
      accountingMethodProperty = accountingMethodProperty,
      overseasPropertyAccountingMethod = overseasPropertyAccountingMethod)

  def testCacheMap(incomeSource: Option[IncomeSourceModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   ukPropertyStartDate: Option[PropertyStartDateModel] = None,
                   accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                   overseasPropertyStartDate: Option[OverseasPropertyStartDateModel] = None,
                   overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(SelectedTaxYear -> AccountingYearModel.format.writes(model))) ++
      ukPropertyStartDate.fold(emptyMap)(model => Map(PropertyStartDate -> PropertyStartDateModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      accountingMethodProperty.fold(emptyMap)(model => Map(PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model))) ++
      overseasPropertyStartDate.fold(emptyMap)(model => Map(OverseasPropertyStartDate -> OverseasPropertyStartDateModel.format.writes(model))) ++
      overseasPropertyAccountingMethod.fold(emptyMap)(model => Map(OverseasPropertyAccountingMethod -> OverseasAccountingMethodPropertyModel.format.writes(model)))
    CacheMap("", map)
  }

  // individual
  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(true, false, false)
  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)
  lazy val testIncomeSourceOverseasProperty: IncomeSourceModel = IncomeSourceModel(false, false, true)
  lazy val testIncomeSourceBoth: IncomeSourceModel = IncomeSourceModel(true, true, false)
  lazy val testIncomeSourceAll: IncomeSourceModel = IncomeSourceModel(true, true, true)

  //agent
  lazy val testAgentIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(true, false, false)
  lazy val testAgentIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)
  lazy val testAgentIncomeSourceBusinessProperty: IncomeSourceModel = IncomeSourceModel(true, true, false)
  lazy val testAgentIncomeSourceBusinessOverseasProperty: IncomeSourceModel = IncomeSourceModel(true, false, true)
  lazy val testAgentIncomeSourceUkPropertyOverseasProperty: IncomeSourceModel = IncomeSourceModel(false, true, true)
  lazy val testAgentIncomeSourceAll: IncomeSourceModel = IncomeSourceModel(true, true, true)
  lazy val testAgentIncomeSourceForeignProperty: IncomeSourceModel = IncomeSourceModel(false, false, true)


  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, Some(testUtr))

  lazy val testMatchNoUtrModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, None)

  lazy val testSummaryDataProperty = IndividualSummary(
    incomeSource = testIncomeSourceProperty,
    accountingMethodProperty = testAccountingMethodProperty
  )

  lazy val testSummaryDataBusinessNextTaxYear = IndividualSummary(
    incomeSource = testIncomeSourceBusiness,
    businessName = testBusinessName,
    selectedTaxYear = testSelectedTaxYearNext,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataBusiness = IndividualSummary(
    incomeSource = testIncomeSourceBusiness,
    businessName = testBusinessName,
    selectedTaxYear = None,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataSelfEmploymentData =
    Seq(SelfEmploymentData
    (
      id = testId,
      businessStartDate = testBusinessStartDate,
      businessName = testBusinessName,
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(testBusinessAddressModel)
    )
    )

  lazy val testSummaryData = IndividualSummary(
    testIncomeSourceBoth,
    businessName = testBusinessName,
    accountingMethod = testAccountingMethod,
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  lazy val testAgentSummaryData = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceAll),
    businessName = Some(testBusinessName),
    accountingMethod = Some(testAccountingMethod),
    accountingMethodProperty = Some(testAccountingMethodProperty),
    propertyStartDate = Some(testPropertyStartDateModel),
    overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel),
    overseasAccountingMethodProperty = Some(testOverseasAccountingMethodProperty)
  )

  lazy val testAgentSummaryDataBusiness = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceBusiness),
    businessName = Some(testBusinessName),
    accountingMethod = Some(testAccountingMethod)
  )

  lazy val testAgentSummaryDataProperty = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceProperty),
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  lazy val testAgentSummaryDataBoth = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceBusinessProperty),
    businessName = Some(testBusinessName),
    accountingMethod = Some(testAccountingMethod),
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  lazy val testAgentSummaryDataNextTaxYear = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceBusiness),
    businessName = Some(testBusinessName),
    selectedTaxYear = Some(testSelectedTaxYearNext),
    accountingMethod = Some(testAccountingMethod)
  )

}
