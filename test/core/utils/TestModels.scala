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

package core.utils


import java.time.LocalDate

import agent.models.AccountingPeriodPriorModel
import agent.services.CacheConstants.AccountingPeriodPrior
import core.models._
import core.services.CacheConstants
import core.utils.TestConstants._
import incometax.business.models._
import incometax.business.models.address.{Address, Country, ReturnedAddress}
import incometax.incomesource.models._
import incometax.subscription.models._
import incometax.unauthorisedagent.models.StoredSubscription
import play.api.libs.json.{JsValue, Json}
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
  val testAccountingPeriodPriorCurrent: AccountingPeriodPriorModel = AccountingPeriodPriorModel(No)
  val testAccountingPeriodPriorNext: AccountingPeriodPriorModel = AccountingPeriodPriorModel(Yes)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

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
  val testTerms = true

  val emptyCacheMap = CacheMap("", Map())

  lazy val testCacheMap: CacheMap =
    testCacheMap(
      incomeSource = testIncomeSourceBoth,
      rentUkProperty = testRentUkProperty_property_and_other,
      areYouSelfEmployed = testAreYouSelfEmployed_yes,
      otherIncome = testOtherIncomeNo,
      matchTaxYear = testMatchTaxYearNo,
      accountingPeriodPrior = testAccountingPeriodPriorCurrent,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      businessPhoneNumber = testBusinessPhoneNumber,
      businessAddress = testAddress,
      businessStartDate = testBusinessStartDate,
      selectedTaxYear = testSelectedTaxYearNext,
      accountingMethod = testAccountingMethod,
      accountingMethodProperty = testAccountingMethodProperty,
      terms = testTerms
    )

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceType] = testIncomeSourceBoth,
                          rentUkProperty: Option[RentUkPropertyModel] = testRentUkProperty_property_and_other,
                          areYouSelfEmployed: Option[AreYouSelfEmployedModel] = testAreYouSelfEmployed_yes,
                          otherIncome: Option[YesNo] = testOtherIncomeNo,
                          matchTaxYear: Option[MatchTaxYearModel] = testMatchTaxYearNo,
                          accountingPeriodPrior: Option[AccountingPeriodPriorModel] = testAccountingPeriodPriorCurrent,
                          accountingPeriodDate: Option[AccountingPeriodModel] = testAccountingPeriod,
                          businessName: Option[BusinessNameModel] = testBusinessName,
                          businessPhoneNumber: Option[BusinessPhoneNumberModel] = testBusinessPhoneNumber,
                          businessAddress: Option[Address] = testAddress,
                          businessStartDate: Option[BusinessStartDateModel] = testBusinessStartDate,
                          selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYearNext,
                          accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                          accountingMethodProperty: Option[AccountingMethodPropertyModel] = testAccountingMethodProperty,
                          terms: Option[Boolean] = testTerms): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      rentUkProperty = rentUkProperty,
      areYouSelfEmployed = areYouSelfEmployed,
      otherIncome = otherIncome,
      matchTaxYear = matchTaxYear,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      businessPhoneNumber = businessPhoneNumber,
      businessAddress = businessAddress,
      businessStartDate = businessStartDate,
      selectedTaxYear = selectedTaxYear,
      accountingMethod = accountingMethod,
      accountingMethodProperty = accountingMethodProperty,
      terms = terms)

  def testCacheMap(incomeSource: Option[IncomeSourceType] = None,
                   rentUkProperty: Option[RentUkPropertyModel] = None,
                   areYouSelfEmployed: Option[AreYouSelfEmployedModel] = None,
                   otherIncome: Option[YesNo] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   accountingPeriodPrior: Option[AccountingPeriodPriorModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                   businessAddress: Option[Address] = None,
                   businessStartDate: Option[BusinessStartDateModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                   terms: Option[Boolean] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceType.format.writes(model))) ++
      rentUkProperty.fold(emptyMap)(model => Map(RentUkProperty -> RentUkPropertyModel.format.writes(model))) ++
      areYouSelfEmployed.fold(emptyMap)(model => Map(AreYouSelfEmployed -> AreYouSelfEmployedModel.format.writes(model))) ++
      otherIncome.fold(emptyMap)(model => Map(OtherIncome -> YesNo.format.writes(model))) ++
      accountingPeriodPrior.fold(emptyMap)(model => Map(AccountingPeriodPrior -> AccountingPeriodPriorModel.format.writes(model))) ++
      matchTaxYear.fold(emptyMap)(model => Map(MatchTaxYear -> MatchTaxYearModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      businessPhoneNumber.fold(emptyMap)(model => Map(BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model))) ++
      businessAddress.fold(emptyMap)(model => Map(BusinessAddress -> Address.format.writes(model))) ++
      businessStartDate.fold(emptyMap)(model => Map(BusinessStartDate -> BusinessStartDateModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(SelectedTaxYear -> AccountingYearModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      accountingMethodProperty.fold(emptyMap)(model => Map(PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model))) ++
      terms.fold(emptyMap)(model => Map(Terms -> Json.toJson(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness = Business

  lazy val testIncomeSourceProperty = Property

  lazy val testIncomeSourceBoth = Both

  lazy val testRentUkProperty_no_property = RentUkPropertyModel(No, None)
  lazy val testRentUkProperty_property_only = RentUkPropertyModel(Yes, Yes)
  lazy val testRentUkProperty_property_and_other = RentUkPropertyModel(Yes, No)

  lazy val testAreYouSelfEmployed_yes = AreYouSelfEmployedModel(Yes)
  lazy val testAreYouSelfEmployed_no = AreYouSelfEmployedModel(No)

  lazy val testOtherIncomeNo = No

  lazy val testOtherIncomeYes = Yes

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, Some(testUtr))

  lazy val testMatchNoUtrModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, None)

  lazy val testSummaryDataProperty = IndividualSummary(
    rentUkProperty = testRentUkProperty_property_only,
    otherIncome = testOtherIncomeNo,
    matchTaxYear = testMatchTaxYearNo,
    accountingPeriod = testAccountingPeriod,
    businessName = testBusinessName,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataBusinessMatchTaxYear = IndividualSummary(
    rentUkProperty = testRentUkProperty_no_property,
    areYouSelfEmployed = testAreYouSelfEmployed_yes,
    otherIncome = testOtherIncomeNo,
    matchTaxYear = testMatchTaxYearYes,
    businessName = testBusinessName,
    selectedTaxYear = testSelectedTaxYearCurrent,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryDataBusiness = IndividualSummary(
    rentUkProperty = testRentUkProperty_no_property,
    areYouSelfEmployed = testAreYouSelfEmployed_yes,
    otherIncome = testOtherIncomeNo,
    matchTaxYear = testMatchTaxYearNo,
    accountingPeriod = testAccountingPeriod,
    businessName = testBusinessName,
    selectedTaxYear = None,
    accountingMethod = testAccountingMethod
  )

  lazy val testSummaryData = IndividualSummary(
    rentUkProperty = testRentUkProperty_property_and_other,
    areYouSelfEmployed = testAreYouSelfEmployed_yes,
    otherIncome = testOtherIncomeNo,
    matchTaxYear = testMatchTaxYearNo,
    accountingPeriod = testAccountingPeriod,
    businessName = testBusinessName,
    accountingMethod = testAccountingMethod
  )

  lazy val testAgentSummaryData = AgentSummary(
    incomeSource = Some(testIncomeSourceBusiness),
    otherIncome = Some(testOtherIncomeNo),
    matchTaxYear = Some(testMatchTaxYearYes),
    accountingPeriodPrior = None,
    accountingPeriod = None,
    businessName = Some(testBusinessName),
    businessPhoneNumber = Some(testBusinessPhoneNumber),
    businessAddress = Some(testAddress),
    businessStartDate = Some(testBusinessStartDate),
    accountingMethod = Some(testAccountingMethod),
    terms = Some(true)
  )

  lazy val testStoredSubscription = StoredSubscription(
    arn = testArn,
    incomeSource = Both,
    otherIncome = false,
    currentPeriodIsPrior = Some(true),
    accountingPeriodStart = Some(testAccountingPeriod.startDate),
    accountingPeriodEnd = Some(testAccountingPeriod.endDate),
    tradingName = Some(testBusinessName.businessName),
    cashOrAccruals = Some(testAccountingMethod.accountingMethod)
  )

}
