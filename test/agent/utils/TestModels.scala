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

package agent.utils

import agent.services.CacheConstants
import core.utils.Implicits
import models.individual.business._
import models.individual.subscription.{Both, Business, IncomeSourceType, Property}
import models.usermatching.UserDetailsModel
import models._
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.cache.client.CacheMap


object TestModels extends Implicits {

  import CacheConstants._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate: DateModel = core.utils.TestModels.testStartDate
  val testEndDate: DateModel = core.utils.TestModels.testEndDate
  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(Yes)
  val testMatchTaxYearNo: MatchTaxYearModel = MatchTaxYearModel(No)
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
      incomeSource = testIncomeSourceBoth,
      matchTaxYear = testMatchTaxYearNo,
      selectedTaxYear = testSelectedTaxYearNext,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      accountingMethod = testAccountingMethod,
      accountingMethodProperty = testAccountingMethodProperty)

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceType] = testIncomeSourceBoth,
                          matchTaxYear: MatchTaxYearModel = testMatchTaxYearNo,
                          selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYearNext,
                          accountingPeriodDate: Option[AccountingPeriodModel] = testAccountingPeriod,
                          businessName: Option[BusinessNameModel] = testBusinessName,
                          accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                          accountingMethodProperty: Option[AccountingMethodPropertyModel] = testAccountingMethodProperty): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      matchTaxYear = matchTaxYear,
      selectedTaxYear = selectedTaxYear,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      accountingMethod = accountingMethod,
      accountingMethodProperty = accountingMethodProperty)

  def testCacheMap(incomeSource: Option[IncomeSourceType] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   accountingMethodProperty: Option[AccountingMethodPropertyModel] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceType.format.writes(model))) ++
      matchTaxYear.fold(emptyMap)(model => Map(MatchTaxYear -> MatchTaxYearModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(WhatYearToSignUp -> AccountingYearModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      accountingMethodProperty.fold(emptyMap)(model => Map(AccountingMethodProperty -> AccountingMethodPropertyModel.format.writes(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness: IncomeSourceType = Business

  lazy val testIncomeSourceProperty: IncomeSourceType = Property

  lazy val testIncomeSourceBoth: IncomeSourceType = Both

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  // TODO change when consolidating models
  lazy val testClientDetails = UserDetailsModel("Test", "User", TestConstants.testNino, DateModel("01", "04", "2017"))

}
