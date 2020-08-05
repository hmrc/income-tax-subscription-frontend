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

package utilities

import java.time.LocalDate

import models._
import models.common._
import models.individual.business._
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription._
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

  val testStartDate = DateModel("06", "04", LocalDate.now.getYear.toString)
  val testEndDate = DateModel("01", "04", LocalDate.now.plusYears(1).getYear.toString)

  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(Yes)
  val testMatchTaxYearNo: MatchTaxYearModel = MatchTaxYearModel(No)
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
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
  val testAccountingMethodOverseasProperty = OverseasAccountingMethodPropertyModel(Cash)

  val emptyCacheMap = CacheMap("", Map())

  lazy val testCacheMap: CacheMap =
    testCacheMap(
      incomeSourceIndiv = testIncomeSourceBoth,
      incomeSource = testAgentIncomeSourceBoth,
      matchTaxYear = testMatchTaxYearNo,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      selectedTaxYear = testSelectedTaxYearNext,
      accountingMethod = testAccountingMethod,
      accountingMethodProperty = testAccountingMethodProperty
    )

  def testCacheMapCustom(incomeSourceIndiv: Option[IncomeSourceModel] = testIncomeSourceBoth,
                         incomeSource: Option[IncomeSourceType] = testAgentIncomeSourceBoth,
                         matchTaxYear: Option[MatchTaxYearModel] = testMatchTaxYearNo,
                         accountingPeriodDate: Option[AccountingPeriodModel] = testAccountingPeriod,
                         businessName: Option[BusinessNameModel] = testBusinessName,
                         selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYearNext,
                         accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                         accountingMethodProperty: Option[AccountingMethodPropertyModel] = testAccountingMethodProperty,
                         overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = testAccountingMethodOverseasProperty): CacheMap =
    testCacheMap(
      incomeSourceIndiv = incomeSourceIndiv,
      incomeSource = incomeSource,
      matchTaxYear = matchTaxYear,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      selectedTaxYear = selectedTaxYear,
      accountingMethod = accountingMethod,
      accountingMethodProperty = accountingMethodProperty,
      overseasPropertyAccountingMethod = overseasPropertyAccountingMethod)

  def testCacheMap(incomeSource: Option[IncomeSourceType] = None,
                   incomeSourceIndiv: Option[IncomeSourceModel] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                   overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceType.format.writes(model))) ++
      incomeSourceIndiv.fold(emptyMap)(model => Map(IndividualIncomeSource -> IncomeSourceModel.format.writes(model))) ++
      matchTaxYear.fold(emptyMap)(model => Map(MatchTaxYear -> MatchTaxYearModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(SelectedTaxYear -> AccountingYearModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      accountingMethodProperty.fold(emptyMap)(model => Map(PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model))) ++
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
  lazy val testAgentIncomeSourceBusiness: IncomeSourceType = Business
  lazy val testAgentIncomeSourceProperty: IncomeSourceType = UkProperty
  lazy val testAgentIncomeSourceBoth: IncomeSourceType = Both


  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, Some(testUtr))

  lazy val testMatchNoUtrModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, None)

  lazy val testSummaryDataProperty = IndividualSummary(
    incomeSourceIndiv = testIncomeSourceProperty,
    accountingMethodProperty = testAccountingMethodProperty
  )

  lazy val testSummaryDataBusinessMatchTaxYear = IndividualSummary(
    incomeSourceIndiv = testIncomeSourceBusiness,
    businessName = testBusinessName,
    selectedTaxYear = testSelectedTaxYearCurrent,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataBusinessNextTaxYear = IndividualSummary(
    incomeSourceIndiv = testIncomeSourceBusiness,
    businessName = testBusinessName,
    selectedTaxYear = testSelectedTaxYearNext,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataBusiness = IndividualSummary(
    incomeSourceIndiv = testIncomeSourceBusiness,
    businessName = testBusinessName,
    selectedTaxYear = None,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryData = IndividualSummary(
    testIncomeSourceBoth,
    businessName = testBusinessName,
    accountingMethod = testAccountingMethod,
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  lazy val testAgentSummaryData = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceBoth),
    matchTaxYear = Some(testMatchTaxYearNo),
    accountingPeriodDate = testAccountingPeriod,
    businessName = Some(testBusinessName),
    accountingMethod = Some(testAccountingMethod),
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  lazy val testAgentSummaryDataBusiness = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceBusiness),
    matchTaxYear = Some(testMatchTaxYearNo),
    accountingPeriodDate = testAccountingPeriod,
    businessName = Some(testBusinessName),
    accountingMethod = Some(testAccountingMethod)
  )

  lazy val testAgentSummaryDataProperty = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceProperty),
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  lazy val testAgentSummaryDataBoth = AgentSummary(
    incomeSource = Some(testAgentIncomeSourceBoth),
    matchTaxYear = Some(testMatchTaxYearNo),
    accountingPeriodDate = testAccountingPeriod,
    businessName = Some(testBusinessName),
    accountingMethod = Some(testAccountingMethod),
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  val testValidStartDate = DateModel.dateConvert(LocalDate.now.minusYears(3))
  val testPropertyCommencementDateModel: PropertyCommencementDateModel = PropertyCommencementDateModel(testValidStartDate)
  val testForeignPropertyCommencementDateModel: OverseasPropertyCommencementDateModel = OverseasPropertyCommencementDateModel(testValidStartDate)
}
