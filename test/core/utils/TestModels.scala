/*
 * Copyright 2018 HM Revenue & Customs
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


import agent.forms.AccountingPeriodPriorForm
import agent.models.AccountingPeriodPriorModel
import agent.services.CacheConstants.AccountingPeriodPrior
import core.models.YesNoModel._
import core.models.{DateModel, No, Yes, YesNo}
import core.services.CacheConstants
import core.utils.TestConstants._
import incometax.business.forms.{AccountingMethodForm, MatchTaxYearForm}
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

  val testStartDate = DateModel("06", "04", "2017")
  val testEndDate = DateModel("01", "04", "2018")

  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(MatchTaxYearForm.option_yes)
  val testMatchTaxYearNo: MatchTaxYearModel = MatchTaxYearModel(MatchTaxYearForm.option_no)
  val testAccountingPeriodPriorCurrent: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
  val testAccountingPeriodPriorNext: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)
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
  val testAccountingMethod = AccountingMethodModel(AccountingMethodForm.option_cash)
  val testTerms = true

  val emptyCacheMap = CacheMap("", Map())

  lazy val testCacheMap: CacheMap =
    testCacheMap(incomeSource = testIncomeSourceBoth,
      rentUkProperty = testRentUkProperty_property_and_other,
      workForYourself = testWorkForYourself_yes,
      otherIncome = testOtherIncomeNo,
      matchTaxYear = testMatchTaxYearNo,
      accountingPeriodPrior = testAccountingPeriodPriorCurrent,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      businessPhoneNumber = testBusinessPhoneNumber,
      businessAddress = testAddress,
      businessStartDate = testBusinessStartDate,
      accountingMethod = testAccountingMethod,
      terms = testTerms)

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceType] = testIncomeSourceBoth,
                          rentUkProperty: Option[RentUkPropertyModel] = testRentUkProperty_property_and_other,
                          workForYourself: Option[WorkForYourselfModel] = testWorkForYourself_yes,
                          otherIncome: Option[YesNo] = testOtherIncomeNo,
                          matchTaxYear: Option[MatchTaxYearModel] = testMatchTaxYearNo,
                          accountingPeriodPrior: Option[AccountingPeriodPriorModel] = testAccountingPeriodPriorCurrent,
                          accountingPeriodDate: Option[AccountingPeriodModel] = testAccountingPeriod,
                          businessName: Option[BusinessNameModel] = testBusinessName,
                          businessPhoneNumber: Option[BusinessPhoneNumberModel] = testBusinessPhoneNumber,
                          businessAddress: Option[Address] = testAddress,
                          businessStartDate: Option[BusinessStartDateModel] = testBusinessStartDate,
                          accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                          terms: Option[Boolean] = testTerms): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      rentUkProperty = rentUkProperty,
      workForYourself = workForYourself,
      otherIncome = otherIncome,
      matchTaxYear = matchTaxYear,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      businessPhoneNumber = businessPhoneNumber,
      businessAddress = businessAddress,
      businessStartDate = businessStartDate,
      accountingMethod = accountingMethod,
      terms = terms)

  def testCacheMap(incomeSource: Option[IncomeSourceType] = None,
                   rentUkProperty: Option[RentUkPropertyModel] = None,
                   workForYourself: Option[WorkForYourselfModel] = None,
                   otherIncome: Option[YesNo] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   accountingPeriodPrior: Option[AccountingPeriodPriorModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                   businessAddress: Option[Address] = None,
                   businessStartDate: Option[BusinessStartDateModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   terms: Option[Boolean] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceType.format.writes(model))) ++
      rentUkProperty.fold(emptyMap)(model => Map(RentUkProperty -> RentUkPropertyModel.format.writes(model))) ++
      workForYourself.fold(emptyMap)(model => Map(WorkForYourself -> WorkForYourselfModel.format.writes(model))) ++
      otherIncome.fold(emptyMap)(model => Map(OtherIncome -> YesNo.format.writes(model))) ++
      accountingPeriodPrior.fold(emptyMap)(model => Map(AccountingPeriodPrior -> AccountingPeriodPriorModel.format.writes(model))) ++
      matchTaxYear.fold(emptyMap)(model => Map(MatchTaxYear -> MatchTaxYearModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      businessPhoneNumber.fold(emptyMap)(model => Map(BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model))) ++
      businessAddress.fold(emptyMap)(model => Map(BusinessAddress -> Address.format.writes(model))) ++
      businessStartDate.fold(emptyMap)(model => Map(BusinessStartDate -> BusinessStartDateModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      terms.fold(emptyMap)(model => Map(Terms -> Json.toJson(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness = Business

  lazy val testIncomeSourceProperty = Property

  lazy val testIncomeSourceBoth = Both

  lazy val testRentUkProperty_no_property = RentUkPropertyModel(NO, None)
  lazy val testRentUkProperty_property_only = RentUkPropertyModel(YES, YES)
  lazy val testRentUkProperty_property_and_other = RentUkPropertyModel(YES, NO)

  lazy val testWorkForYourself_yes = WorkForYourselfModel(YES)
  lazy val testWorkForYourself_no = WorkForYourselfModel(NO)

  lazy val testIsCurrentPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

  lazy val testIsNextPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)

  lazy val testOtherIncomeNo = No

  lazy val testOtherIncomeYes = Yes

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, Some(testUtr))

  lazy val testMatchNoUtrModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, None)

  lazy val testSummaryData = IndividualSummary(
    rentUkProperty = testRentUkProperty_property_and_other,
    workForYourself = testWorkForYourself_yes,
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
