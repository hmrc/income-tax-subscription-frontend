/*
 * Copyright 2017 HM Revenue & Customs
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


import core.services.CacheConstants
import core.utils.TestConstants._
import forms._
import incometax.business.models.{BusinessNameModel, BusinessPhoneNumberModel, BusinessStartDateModel}
import incometax.business.models.address.{Address, Country, ReturnedAddress}
import models._
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

  val testAccountingPeriodPriorCurrent: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
  val testAccountingPeriodPriorNext: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

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

  val testCacheMap: CacheMap =
    testCacheMap(incomeSource = testIncomeSourceBoth,
      otherIncome = testOtherIncomeNo,
      accountingPeriodPrior = testAccountingPeriodPriorCurrent,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      businessPhoneNumber = testBusinessPhoneNumber,
      businessAddress = testAddress,
      businessStartDate = testBusinessStartDate,
      accountingMethod = testAccountingMethod,
      terms = testTerms)

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceModel] = testIncomeSourceBoth,
                          otherIncome: Option[OtherIncomeModel] = testOtherIncomeNo,
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
      otherIncome = otherIncome,
      accountingPeriodPrior = accountingPeriodPrior,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      businessPhoneNumber = businessPhoneNumber,
      businessAddress = businessAddress,
      businessStartDate = businessStartDate,
      accountingMethod = accountingMethod,
      terms = terms)

  def testCacheMap(incomeSource: Option[IncomeSourceModel] = None,
                   otherIncome: Option[OtherIncomeModel] = None,
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
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceModel.format.writes(model))) ++
      otherIncome.fold(emptyMap)(model => Map(OtherIncome -> OtherIncomeModel.format.writes(model))) ++
      accountingPeriodPrior.fold(emptyMap)(model => Map(AccountingPeriodPrior -> AccountingPeriodPriorModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      businessPhoneNumber.fold(emptyMap)(model => Map(BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model))) ++
      businessAddress.fold(emptyMap)(model => Map(BusinessAddress -> Address.format.writes(model))) ++
      businessStartDate.fold(emptyMap)(model => Map(BusinessStartDate -> BusinessStartDateModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      terms.fold(emptyMap)(model => Map(Terms -> Json.toJson(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness = IncomeSourceModel(IncomeSourceForm.option_business)

  lazy val testIncomeSourceProperty = IncomeSourceModel(IncomeSourceForm.option_property)

  lazy val testIncomeSourceBoth = IncomeSourceModel(IncomeSourceForm.option_both)

  lazy val testIsCurrentPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

  lazy val testIsNextPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)

  lazy val testOtherIncomeNo = OtherIncomeModel(OtherIncomeForm.option_no)

  lazy val testOtherIncomeYes = OtherIncomeModel(OtherIncomeForm.option_yes)

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, Some(testUtr))

  lazy val testMatchNoUtrModel = UserMatchSuccessResponseModel(testFirstName, testLastName, TestConstants.testNino, testNino, None)

  val testSummaryData = SummaryModel(
    incomeSource = IncomeSourceModel(IncomeSourceForm.option_both),
    otherIncome = OtherIncomeModel(OtherIncomeForm.option_no),
    accountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no),
    accountingPeriod = AccountingPeriodModel(TestConstants.startDate, TestConstants.endDate),
    businessName = BusinessNameModel("ABC"),
    accountingMethod = AccountingMethodModel("Cash")
  )

}
