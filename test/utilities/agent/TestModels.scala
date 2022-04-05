/*
 * Copyright 2022 HM Revenue & Customs
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

package utilities.agent


import models.common._
import models.common.business._
import models.usermatching.UserDetailsModel
import models.{AccountingMethod => _, _}
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys._

object TestModels {

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate: DateModel = utilities.TestModels.testStartDate
  val testEndDate: DateModel = utilities.TestModels.testEndDate
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
  val testSelectedTaxYearCurrent = AccountingYearModel(Current)
  val testSelectedTaxYearNext = AccountingYearModel(Next)
  val testSelfEmployments = Some(Seq(SelfEmploymentData("1", Some(BusinessStartDate(testStartDate)), Some(testBusinessName),
    Some(BusinessTradeNameModel("plumbing")), confirmed = true)))

  val emptyCacheMap = CacheMap("", Map())

  val testCacheMap: CacheMap =
    testCacheMap(
      incomeSource = Some(testIncomeSourceBusinessAndUkProperty),
      selectedTaxYear = Some(testSelectedTaxYearNext),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod))

  val testCurrentCacheMap: CacheMap =
    testCacheMap(
      incomeSource = Some(testIncomeSourceBusinessAndUkProperty),
      selectedTaxYear = Some(testSelectedTaxYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod))

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceModel] = Some(testIncomeSourceBusinessAndUkProperty),
                          selectedTaxYear: Option[AccountingYearModel] = Some(testSelectedTaxYearNext),
                          businessName: Option[BusinessNameModel] = Some(testBusinessName),
                          accountingMethod: Option[AccountingMethodModel] = Some(testAccountingMethod)): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      selectedTaxYear = selectedTaxYear,
      businessName = businessName,
      accountingMethod = accountingMethod)

  def testCacheMap(incomeSource: Option[IncomeSourceModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(SelectedTaxYear -> AccountingYearModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(true, false, false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)

  lazy val testIncomeSourceOverseasProperty: IncomeSourceModel = IncomeSourceModel(false, false, true)

  lazy val testIncomeSourceBusinessAndUkProperty: IncomeSourceModel = IncomeSourceModel(true, true, false)

  lazy val testIncomeSourceNone: IncomeSourceModel = IncomeSourceModel(false, false, false)

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  // TODO change when consolidating models
  lazy val testClientDetails = UserDetailsModel("Test", "User", TestConstants.testNino, DateModel("01", "04", "2017"))

}
