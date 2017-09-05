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

package utils


import connectors.models.matching.UserMatchSuccessResponseModel
import forms._
import models._
import models.matching.UserDetailsModel
import play.api.libs.json.{JsValue, Json}
import services.CacheConstants
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.TestConstants._


object TestModels extends Implicits {

  import CacheConstants._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate = DateModel("01", "04", "2017")
  val testEndDate = DateModel("01", "04", "2018")
  val testAccountingPeriodPriorCurrent: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)
  val testAccountingPeriodPriorNext: AccountingPeriodPriorModel = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testAccountingMethod = AccountingMethodModel(AccountingMethodForm.option_cash)
  val testTerms = true

  val emptyCacheMap = CacheMap("", Map())

  val testCacheMap: CacheMap =
    testCacheMap(incomeSource = testIncomeSourceBoth,
      otherIncome = testOtherIncomeNo,
      accountingPeriodPrior = testAccountingPeriodPriorCurrent,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      accountingMethod = testAccountingMethod,
      terms = testTerms)

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceModel] = testIncomeSourceBoth,
                          otherIncome: Option[OtherIncomeModel] = testOtherIncomeNo,
                          accountingPeriodPrior: Option[AccountingPeriodPriorModel] = testAccountingPeriodPriorCurrent,
                          accountingPeriodDate: Option[AccountingPeriodModel] = testAccountingPeriod,
                          businessName: Option[BusinessNameModel] = testBusinessName,
                          accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                          terms: Option[Boolean] = testTerms): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      otherIncome = otherIncome,
      accountingPeriodPrior = accountingPeriodPrior,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      accountingMethod = accountingMethod,
      terms = terms)

  def testCacheMap(incomeSource: Option[IncomeSourceModel] = None,
                   otherIncome: Option[OtherIncomeModel] = None,
                   accountingPeriodPrior: Option[AccountingPeriodPriorModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   terms: Option[Boolean] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceModel.format.writes(model))) ++
      otherIncome.fold(emptyMap)(model => Map(OtherIncome -> OtherIncomeModel.format.writes(model))) ++
      accountingPeriodPrior.fold(emptyMap)(model => Map(AccountingPeriodPrior -> AccountingPeriodPriorModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      terms.fold(emptyMap)(model => Map(Terms -> Json.toJson(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness = IncomeSourceModel(IncomeSourceForm.option_business)

  lazy val testIncomeSourceOther = IncomeSourceModel(IncomeSourceForm.option_other)

  lazy val testIncomeSourceProperty = IncomeSourceModel(IncomeSourceForm.option_property)

  lazy val testIncomeSourceBoth = IncomeSourceModel(IncomeSourceForm.option_both)

  lazy val testIsCurrentPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

  lazy val testIsNextPeriod = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)

  lazy val testOtherIncomeNo = OtherIncomeModel(OtherIncomeForm.option_no)

  lazy val testOtherIncomeYes = OtherIncomeModel(OtherIncomeForm.option_yes)

  lazy val testUserDetails = UserDetailsModel("Test", "User", TestConstants.testNino, testStartDate)

  lazy val testMatchSuccessModel  = UserMatchSuccessResponseModel("test", "user", "000", testNino, Some("abc"))

  lazy val testMatchNoUtrModel  = UserMatchSuccessResponseModel("test", "user", "000", testNino, None)

  val testSummaryData = SummaryModel(
    incomeSource = IncomeSourceModel(IncomeSourceForm.option_both),
    otherIncome = OtherIncomeModel(OtherIncomeForm.option_no),
    accountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no),
    accountingPeriod = AccountingPeriodModel(TestConstants.startDate, TestConstants.endDate),
    businessName = BusinessNameModel("ABC"),
    accountingMethod = AccountingMethodModel("Cash")
  )
}
