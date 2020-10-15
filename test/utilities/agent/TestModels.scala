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

package utilities.agent


import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingPeriodModel, AccountingYearModel, BusinessNameModel, _}
import models.usermatching.UserDetailsModel
import models.{AccountingMethod => _, _}
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.Implicits
import utilities.SubscriptionDataKeys._

object TestModels extends Implicits {

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

  val emptyCacheMap = CacheMap("", Map())

  val testCacheMap: CacheMap =
    testCacheMap(
      incomeSource = testIncomeSourceBusinessAndUkProperty,
      selectedTaxYear = testSelectedTaxYearNext,
      businessName = testBusinessName,
      accountingMethod = testAccountingMethod,
      accountingMethodProperty = testAccountingMethodProperty)

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceModel] = testIncomeSourceBusinessAndUkProperty,
                          selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYearNext,
                          businessName: Option[BusinessNameModel] = testBusinessName,
                          accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                          accountingMethodProperty: Option[AccountingMethodPropertyModel] = testAccountingMethodProperty): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      selectedTaxYear = selectedTaxYear,
      businessName = businessName,
      accountingMethod = accountingMethod,
      accountingMethodProperty = accountingMethodProperty)

  def testCacheMap(incomeSource: Option[IncomeSourceModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   accountingMethodProperty: Option[AccountingMethodPropertyModel] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(SelectedTaxYear -> AccountingYearModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      accountingMethodProperty.fold(emptyMap)(model => Map(PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(true, false, false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)

  lazy val testIncomeSourceUkProperty: IncomeSourceModel = IncomeSourceModel(false, false, true)

  lazy val testIncomeSourceBusinessAndUkProperty: IncomeSourceModel = IncomeSourceModel(true, true, false)

  lazy val testIncomeSourceNone: IncomeSourceModel = IncomeSourceModel(false, false, false)

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  // TODO change when consolidating models
  lazy val testClientDetails = UserDetailsModel("Test", "User", TestConstants.testNino, DateModel("01", "04", "2017"))

}
