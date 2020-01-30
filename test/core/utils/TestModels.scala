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

package core.utils


import java.time.LocalDate

import core.models._
import core.services.CacheConstants
import core.utils.TestConstants._
import incometax.business.models._
import incometax.business.models.address.{Address, Country, ReturnedAddress}
import incometax.incomesource.models._
import incometax.subscription.models._
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.cache.client.CacheMap
import usermatching.models.{UserDetailsModel, UserMatchSuccessResponseModel}

object TestModels extends Implicits {

  import CacheConstants._

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
  val testBusinessPhoneNumber = BusinessPhoneNumberModel("0")
  val testAddress = Address(Some(List("line1", "line2")), Some("zz111zz"), Some(Country("GB", "United Kingdom")))
  val testReturnedAddress = ReturnedAddress("ref", Some("id"), testAddress)
  val testBusinessStartDate = BusinessStartDateModel(testStartDate)
  val testSelectedTaxYearCurrent = AccountingYearModel(Current)
  val testSelectedTaxYearNext = AccountingYearModel(Next)
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)

  val emptyCacheMap = CacheMap("", Map())

  lazy val testCacheMap: CacheMap =
    testCacheMap(
      incomeSource = testIncomeSourceBoth,
      rentUkProperty = testRentUkProperty_property_and_other,
      areYouSelfEmployed = testAreYouSelfEmployed_yes,
      matchTaxYear = testMatchTaxYearNo,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      businessPhoneNumber = testBusinessPhoneNumber,
      businessAddress = testAddress,
      businessStartDate = testBusinessStartDate,
      selectedTaxYear = testSelectedTaxYearNext,
      accountingMethod = testAccountingMethod,
      accountingMethodProperty = testAccountingMethodProperty
    )

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceType] = testIncomeSourceBoth,
                          rentUkProperty: Option[RentUkPropertyModel] = testRentUkProperty_property_and_other,
                          areYouSelfEmployed: Option[AreYouSelfEmployedModel] = testAreYouSelfEmployed_yes,
                          matchTaxYear: Option[MatchTaxYearModel] = testMatchTaxYearNo,
                          accountingPeriodDate: Option[AccountingPeriodModel] = testAccountingPeriod,
                          businessName: Option[BusinessNameModel] = testBusinessName,
                          businessPhoneNumber: Option[BusinessPhoneNumberModel] = testBusinessPhoneNumber,
                          businessAddress: Option[Address] = testAddress,
                          businessStartDate: Option[BusinessStartDateModel] = testBusinessStartDate,
                          selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYearNext,
                          accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                          accountingMethodProperty: Option[AccountingMethodPropertyModel] = testAccountingMethodProperty): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      rentUkProperty = rentUkProperty,
      areYouSelfEmployed = areYouSelfEmployed,
      matchTaxYear = matchTaxYear,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      businessPhoneNumber = businessPhoneNumber,
      businessAddress = businessAddress,
      businessStartDate = businessStartDate,
      selectedTaxYear = selectedTaxYear,
      accountingMethod = accountingMethod,
      accountingMethodProperty = accountingMethodProperty)

  def testCacheMap(incomeSource: Option[IncomeSourceType] = None,
                   rentUkProperty: Option[RentUkPropertyModel] = None,
                   areYouSelfEmployed: Option[AreYouSelfEmployedModel] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                   businessAddress: Option[Address] = None,
                   businessStartDate: Option[BusinessStartDateModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   accountingMethodProperty: Option[AccountingMethodPropertyModel] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceType.format.writes(model))) ++
      rentUkProperty.fold(emptyMap)(model => Map(RentUkProperty -> RentUkPropertyModel.format.writes(model))) ++
      areYouSelfEmployed.fold(emptyMap)(model => Map(AreYouSelfEmployed -> AreYouSelfEmployedModel.format.writes(model))) ++
      matchTaxYear.fold(emptyMap)(model => Map(MatchTaxYear -> MatchTaxYearModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      businessPhoneNumber.fold(emptyMap)(model => Map(BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model))) ++
      businessAddress.fold(emptyMap)(model => Map(BusinessAddress -> Address.format.writes(model))) ++
      businessStartDate.fold(emptyMap)(model => Map(BusinessStartDate -> BusinessStartDateModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(SelectedTaxYear -> AccountingYearModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      accountingMethodProperty.fold(emptyMap)(model => Map(PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness: IncomeSourceType = Business

  lazy val testIncomeSourceProperty: IncomeSourceType = Property

  lazy val testIncomeSourceBoth: IncomeSourceType = Both

  lazy val testRentUkProperty_no_property = RentUkPropertyModel(No, None)
  lazy val testRentUkProperty_property_only = RentUkPropertyModel(Yes, Yes)
  lazy val testRentUkProperty_property_and_other = RentUkPropertyModel(Yes, No)

  lazy val testAreYouSelfEmployed_yes = AreYouSelfEmployedModel(Yes)
  lazy val testAreYouSelfEmployed_no = AreYouSelfEmployedModel(No)

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, Some(testUtr))

  lazy val testMatchNoUtrModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, None)

  lazy val testSummaryDataProperty = IndividualSummary(
    rentUkProperty = testRentUkProperty_property_only,
    matchTaxYear = testMatchTaxYearNo,
    accountingPeriodDate = testAccountingPeriod,
    businessName = testBusinessName,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataBusinessMatchTaxYear = IndividualSummary(
    rentUkProperty = testRentUkProperty_no_property,
    areYouSelfEmployed = testAreYouSelfEmployed_yes,
    matchTaxYear = testMatchTaxYearYes,
    businessName = testBusinessName,
    selectedTaxYear = testSelectedTaxYearCurrent,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataBusiness = IndividualSummary(
    rentUkProperty = testRentUkProperty_no_property,
    areYouSelfEmployed = testAreYouSelfEmployed_yes,
    matchTaxYear = testMatchTaxYearNo,
    accountingPeriodDate = testAccountingPeriod,
    businessName = testBusinessName,
    selectedTaxYear = None,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryData = IndividualSummary(
    rentUkProperty = testRentUkProperty_property_and_other,
    areYouSelfEmployed = testAreYouSelfEmployed_yes,
    matchTaxYear = testMatchTaxYearNo,
    accountingPeriodDate = testAccountingPeriod,
    businessName = testBusinessName,
    accountingMethod = testAccountingMethod,
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

  lazy val testAgentSummaryData = AgentSummary(
    incomeSource = Some(testIncomeSourceBoth),
    matchTaxYear = Some(testMatchTaxYearNo),
    accountingPeriodDate = testAccountingPeriod,
    businessName = Some(testBusinessName),
    accountingMethod = Some(testAccountingMethod),
    accountingMethodProperty = Some(testAccountingMethodProperty)
  )

}
