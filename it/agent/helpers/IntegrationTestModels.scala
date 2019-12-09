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

package agent.helpers

import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.models._
import _root_.agent.services.CacheConstants
import core.models._
import incometax.business.models.AccountingPeriodModel
import incometax.subscription.models._
import incometax.unauthorisedagent.models.StoredSubscription
import play.api.libs.json.{JsValue, Json}

object IntegrationTestModels {

  import CacheConstants._

  val testStartDate = _root_.helpers.IntegrationTestModels.testStartDate
  val testEndDate = _root_.helpers.IntegrationTestModels.testEndDate
  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(Yes)
  val testAccountingPeriodPriorCurrent: AccountingPeriodPriorModel = AccountingPeriodPriorModel(No)
  val testAccountingPeriodPriorNext: AccountingPeriodPriorModel = AccountingPeriodPriorModel(Yes)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testPropertyAccountingMethod = AccountingMethodPropertyModel(Cash)
  val testTerms = true

  //n.b. this must match the data in fullKeystoreData
  val testStoredSubscription =
    StoredSubscription(
      arn = testARN,
      incomeSource = Both,
      otherIncome = Some(false),
      currentPeriodIsPrior = Some(false),
      accountingPeriodStart = Some(testAccountingPeriod.startDate),
      accountingPeriodEnd = Some(testAccountingPeriod.endDate),
      tradingName = Some(testBusinessName.businessName),
      cashOrAccruals = Some(testAccountingMethod.accountingMethod)
    )

  val fullKeystoreData: Map[String, JsValue] =
    keystoreData(
      incomeSource = Some(testIncomeSourceBoth),
      otherIncome = Some(testOtherIncomeNo),
      matchTaxYear = Some(testMatchTaxYearYes),
      accountingPeriodPrior = Some(testAccountingPeriodPriorCurrent),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      accountingMethodProperty = Some(testPropertyAccountingMethod),
      terms = Some(testTerms)
    )

  def keystoreData(
                    incomeSource: Option[IncomeSourceType] = None,
                    otherIncome: Option[YesNo] = None,
                    matchTaxYear: Option[MatchTaxYearModel] = None,
                    accountingPeriodPrior: Option[AccountingPeriodPriorModel] = None,
                    accountingPeriodDate: Option[AccountingPeriodModel] = None,
                    businessName: Option[BusinessNameModel] = None,
                    accountingMethod: Option[AccountingMethodModel] = None,
                    accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                    terms: Option[Boolean] = None): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceType.format.writes(model)) ++
      otherIncome.map(model => OtherIncome -> YesNo.format.writes(model)) ++
      accountingPeriodPrior.map(model => AccountingPeriodPrior -> AccountingPeriodPriorModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      accountingMethodProperty.map(model => AccountingMethodProperty -> AccountingMethodPropertyModel.format.writes(model)) ++
      terms.map(model => Terms -> Json.toJson(model)) ++
      matchTaxYear.map(model => MatchTaxYear -> MatchTaxYearModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness = Business

  lazy val testIncomeSourceProperty = Property

  lazy val testIncomeSourceBoth = Both

  lazy val testOtherIncomeNo = No

  lazy val testOtherIncomeYes = Yes

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  lazy val testClientDetails = _root_.helpers.IntegrationTestModels.testUserDetails

}
