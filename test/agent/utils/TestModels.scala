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

package agent.utils

import _root_.agent.services.CacheConstants
import agent.models.AccountingPeriodPriorModel
import core.models._
import core.utils.Implicits
import incometax.business.models._
import incometax.subscription.models._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.cache.client.CacheMap
import usermatching.models.UserDetailsModel


object TestModels extends Implicits {

  import CacheConstants._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate = core.utils.TestModels.testStartDate
  val testEndDate = core.utils.TestModels.testEndDate
  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(Yes)
  val testMatchTaxYearNo: MatchTaxYearModel = MatchTaxYearModel(No)
  val testAccountingPeriodPriorCurrent: AccountingPeriodPriorModel = AccountingPeriodPriorModel(No)
  val testAccountingPeriodPriorNext: AccountingPeriodPriorModel = AccountingPeriodPriorModel(Yes)
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
  val testTerms = true

  val emptyCacheMap = CacheMap("", Map())

  val testCacheMap: CacheMap =
    testCacheMap(
      incomeSource = testIncomeSourceBoth,
      otherIncome = testOtherIncomeNo,
      accountingPeriodPrior = testAccountingPeriodPriorCurrent,
      matchTaxYear = testMatchTaxYearNo,
      selectedTaxYear = testSelectedTaxYearNext,
      accountingPeriodDate = testAccountingPeriod,
      businessName = testBusinessName,
      accountingMethod = testAccountingMethod,
      accountingMethodProperty = testAccountingMethodProperty,
      terms = testTerms)

  def testCacheMapCustom(
                          incomeSource: Option[IncomeSourceType] = testIncomeSourceBoth,
                          otherIncome: Option[YesNo] = testOtherIncomeNo,
                          accountingPeriodPrior: Option[AccountingPeriodPriorModel] = testAccountingPeriodPriorCurrent,
                          matchTaxYear: MatchTaxYearModel = testMatchTaxYearNo,
                          selectedTaxYear: Option[AccountingYearModel] = testSelectedTaxYearNext,
                          accountingPeriodDate: Option[AccountingPeriodModel] = testAccountingPeriod,
                          businessName: Option[BusinessNameModel] = testBusinessName,
                          accountingMethod: Option[AccountingMethodModel] = testAccountingMethod,
                          accountingMethodProperty: Option[AccountingMethodPropertyModel] = testAccountingMethodProperty,
                          terms: Option[Boolean] = testTerms): CacheMap =
    testCacheMap(
      incomeSource = incomeSource,
      otherIncome = otherIncome,
      accountingPeriodPrior = accountingPeriodPrior,
      matchTaxYear = matchTaxYear,
      selectedTaxYear = selectedTaxYear,
      accountingPeriodDate = accountingPeriodDate,
      businessName = businessName,
      accountingMethod = accountingMethod,
      accountingMethodProperty = accountingMethodProperty,
      terms = terms)

  def testCacheMap(incomeSource: Option[IncomeSourceType] = None,
                   otherIncome: Option[YesNo] = None,
                   accountingPeriodPrior: Option[AccountingPeriodPriorModel] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                   terms: Option[Boolean] = None): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceType.format.writes(model))) ++
      otherIncome.fold(emptyMap)(model => Map(OtherIncome -> YesNo.format.writes(model))) ++
      accountingPeriodPrior.fold(emptyMap)(model => Map(AccountingPeriodPrior -> AccountingPeriodPriorModel.format.writes(model))) ++
      matchTaxYear.fold(emptyMap)(model => Map(MatchTaxYear -> MatchTaxYearModel.format.writes(model))) ++
      selectedTaxYear.fold(emptyMap)(model => Map(WhatYearToSignUp -> AccountingYearModel.format.writes(model))) ++
      accountingPeriodDate.fold(emptyMap)(model => Map(AccountingPeriodDate -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      accountingMethod.fold(emptyMap)(model => Map(AccountingMethod -> AccountingMethodModel.format.writes(model))) ++
      accountingMethodProperty.fold(emptyMap)(model => Map(AccountingMethodProperty -> AccountingMethodPropertyModel.format.writes(model))) ++
      terms.fold(emptyMap)(model => Map(Terms -> Json.toJson(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness = Business

  lazy val testIncomeSourceProperty = Property

  lazy val testIncomeSourceBoth = Both

  lazy val testOtherIncomeNo = No

  lazy val testOtherIncomeYes = Yes

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  // TODO change when consolidating models
  lazy val testClientDetails = UserDetailsModel("Test", "User", TestConstants.testNino, core.models.DateModel("01", "04", "2017"))

}
