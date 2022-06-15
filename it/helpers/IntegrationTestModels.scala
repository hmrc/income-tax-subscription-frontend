/*
 * Copyright 2021 HM Revenue & Customs
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

package helpers

import helpers.IntegrationTestConstants._
import models._
import models.common._
import models.common.business._
import models.common.subscription.EnrolmentKey
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import utilities.AccountingPeriodUtil
import _root_.common.Constants._
import _root_.common.Constants.ITSASessionKeys.MTDITID

import java.time.LocalDate

object IntegrationTestModels {

  import utilities.SubscriptionDataKeys._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate: DateModel = DateModel.dateConvert(LocalDate.now)
  val testOneDayAgo: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(1))
  val testEndDate: DateModel = DateModel.dateConvert(LocalDate.now.plusYears(1).plusDays(-1))
  val testEndDateNext: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1).plusDays(-1)
  val testEndDatePlus1Y: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1)
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingYearCurrentConfirmed: AccountingYearModel = AccountingYearModel(Current, confirmed = true)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName: BusinessNameModel = BusinessNameModel("test business")
  val testBusinessTrade: BusinessTradeNameModel = BusinessTradeNameModel("test trade")
  val testBusinessAddress: BusinessAddressModel = BusinessAddressModel(
    "",
    Address(
      lines = Seq(
        "1 long road",
        "lonely town",
        "quiet county"
      ),
      postcode = Some("ZZ1 1ZZ")
    )
  )
  val testAccountingMethod: AccountingMethodModel = AccountingMethodModel(Cash)
  val testAccountingMethodProperty: AccountingMethodPropertyModel = AccountingMethodPropertyModel(Cash)
  val testAccountingMethodForeignProperty: OverseasAccountingMethodPropertyModel = OverseasAccountingMethodPropertyModel(Cash)
  val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
  val testValidStartDate2: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
  val testInvalidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(364))
  val testPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testValidStartDate)
  val testPropertyStartDateModel: PropertyStartDateModel = PropertyStartDateModel(DateModel("05", "04", "2017"))
  val testOverseasPropertyStartDate: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testValidStartDate)
  val testOverseasPropertyStartDateModel: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(DateModel("05", "04", "2017"))
  val testInvalidPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testInvalidStartDate)
  val testBusinesses: Option[Seq[SelfEmploymentData]] = Some(Seq(SelfEmploymentData(
    id = "12345",
    businessStartDate = Some(BusinessStartDate(DateModel("05", "04", "2017"))),
    businessName = Some(testBusinessName),
    businessTradeName = Some(testBusinessTrade),
    businessAddress = Some(testBusinessAddress)
  )))
  val testTooManyBusinesses: Seq[SelfEmploymentData] = Array.range(1, 51).map( i =>
    SelfEmploymentData(
    id = i.toString,
    businessStartDate = Some(BusinessStartDate(DateModel("05", "04", "2017"))),
    businessName = Some(BusinessNameModel(s"${testBusinessName.businessName} $i")),
    businessTradeName = Some(testBusinessTrade),
    businessAddress = Some(testBusinessAddress)
  )).toSeq
  val testInvalidOverseasPropertyStartDate: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testInvalidStartDate)
  val testFullPropertyModel: PropertyModel = PropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    startDate = Some(testPropertyStartDate.startDate),
    confirmed = true
  )

  val testFullOverseasPropertyModel: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    startDate = Some(testPropertyStartDateModel.startDate),
    confirmed = true
  )

  val testBusinessTradeName: BusinessTradeNameModel = BusinessTradeNameModel("test trade name")
  val testBusinessStartDate: BusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), Some("TF2 1PF")))
  val testId = "testId"

  lazy val fullSubscriptionDataBothPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod)
    )

  lazy val fullSubscriptionDataAllPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod)
    )

  lazy val fullSubscriptionDataPropertyPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false))
    )

  val selfEmploymentSubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceBusiness),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod)
    )

  val ukPropertySubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceProperty),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = None,
      accountingMethod = None
    )

  val overseasPropertySubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceOverseas),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = None,
      accountingMethod = None
    )

  val AllSubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceAll),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod)
    )


  def subscriptionData(incomeSource: Option[IncomeSourceModel] = None,
                       selectedTaxYear: Option[AccountingYearModel] = None,
                       businessName: Option[BusinessNameModel] = None,
                       accountingMethod: Option[AccountingMethodModel] = None,
                       ukProperty: Option[PropertyModel] = None,
                       overseasProperty: Option[OverseasPropertyModel] = None,
                      ): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      selectedTaxYear.map(model => SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      ukProperty.map(model => Property -> PropertyModel.format.writes(model)) ++
      overseasProperty.map(model => Property -> OverseasPropertyModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceOverseas: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

  lazy val testIncomeSourceAll: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  lazy val testIncomeSourceIndivProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)

  lazy val testUserDetails: UserDetailsModel = UserDetailsModel(testFirstName, testLastName, testNino, testOneDayAgo)

  lazy val testMTDITEnrolmentKey: EnrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMtdId)
  lazy val testIRSAEnrolmentKey: EnrolmentKey = EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr)

  lazy val testSummaryDataSelfEmploymentData =
    Seq(SelfEmploymentData(
      id = testId,
      businessStartDate = Some(testBusinessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), Some("TF2 1PF"))))
    ))
}
