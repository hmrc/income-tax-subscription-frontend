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


import forms._
import models._
import play.api.libs.json.JsValue
import services.CacheConstants
import uk.gov.hmrc.http.cache.client.CacheMap


object TestModels extends Implicits {

  import CacheConstants._

  val testStartDate = DateModel("01", "04", "2017")
  val testEndDate = DateModel("01", "04", "2018")
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testContactEmail = EmailModel("test@example.com")
  val testIncomeType = AccountingMethodModel(AccountingMethodForm.option_cash)
  val testTerms = TermModel(true)

  val emptyCacheMap = CacheMap("", Map())

  val testCacheMap: CacheMap =
    testCacheMap(
      incomeSource = testIncomeSourceBoth,
      accountingPeriod = testAccountingPeriod,
      businessName = testBusinessName,
      incomeType = testIncomeType,
      contactEmail = testContactEmail,
      terms = testTerms
    )

  def testCacheMap(incomeSource: Option[IncomeSourceModel] = None,
                   accountingPeriod: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   incomeType: Option[AccountingMethodModel] = None,
                   contactEmail: Option[EmailModel] = None,
                   terms: Option[TermModel] = None
                  ): CacheMap = {
    val emptyMap = Map[String, JsValue]()
    val map: Map[String, JsValue] = Map[String, JsValue]() ++
      incomeSource.fold(emptyMap)(model => Map(IncomeSource -> IncomeSourceModel.format.writes(model))) ++
      accountingPeriod.fold(emptyMap)(model => Map(AccountingPeriod -> AccountingPeriodModel.format.writes(model))) ++
      businessName.fold(emptyMap)(model => Map(BusinessName -> BusinessNameModel.format.writes(model))) ++
      incomeType.fold(emptyMap)(model => Map(IncomeType -> AccountingMethodModel.format.writes(model))) ++
      contactEmail.fold(emptyMap)(model => Map(ContactEmail -> EmailModel.format.writes(model))) ++
      terms.fold(emptyMap)(model => Map(Terms -> TermModel.format.writes(model)))
    CacheMap("", map)
  }

  lazy val testIncomeSourceBusiness = IncomeSourceModel(IncomeSourceForm.option_business)

  lazy val testIncomeSourceOther = IncomeSourceModel(IncomeSourceForm.option_other)

  lazy val testIncomeSourceProperty = IncomeSourceModel(IncomeSourceForm.option_property)

  lazy val testIncomeSourceBoth = IncomeSourceModel(IncomeSourceForm.option_both)

  lazy val testIsCurrentPeriod = AccoutingPeriodPriorModel(AccountingPeriodPriorForm.option_no)

  lazy val testIsNextPeriod = AccoutingPeriodPriorModel(AccountingPeriodPriorForm.option_yes)

  lazy val testOtherIncomeNo = OtherIncomeModel(OtherIncomeForm.option_no)

  lazy val testOtherIncomeYes = OtherIncomeModel(OtherIncomeForm.option_yes)

}
