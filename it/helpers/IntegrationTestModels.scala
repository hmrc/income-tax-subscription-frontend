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

import _root_.common.Constants.ITSASessionKeys.MTDITID
import _root_.common.Constants._
import helpers.IntegrationTestConstants._
import models._
import models.common._
import models.common.business._
import models.common.subscription.EnrolmentKey
import models.usermatching.UserDetailsModel
import uk.gov.hmrc.crypto.ApplicationCrypto

import java.time.LocalDate

object IntegrationTestModels extends ComponentSpecBase {

  val crypto : ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]

  val testStartDate: DateModel = DateModel.dateConvert(LocalDate.now)
  private val testOneDayAgo: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(1))
  val testEndDate: DateModel = DateModel.dateConvert(LocalDate.now.plusYears(1).plusDays(-1))
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingYearCurrentConfirmed: AccountingYearModel = AccountingYearModel(Current, confirmed = true)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingYearNextConfirmed: AccountingYearModel = AccountingYearModel(Next, confirmed = true)

  private val testBusinessName: BusinessNameModel = BusinessNameModel("test business")
  private val testBusinessTrade: BusinessTradeNameModel = BusinessTradeNameModel("test trade")
  private val testBusinessAddress: BusinessAddressModel = BusinessAddressModel(
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
  private val testAccountingMethodProperty: AccountingMethodPropertyModel = AccountingMethodPropertyModel(Cash)
  val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
  val testValidStartDate2: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
  //noinspection ScalaStyle
  val testInvalidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(364))
  val testPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testValidStartDate)
  private val testPropertyStartDateModel: PropertyStartDateModel = PropertyStartDateModel(DateModel("05", "04", "2017"))
  val testInvalidPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testInvalidStartDate)
  val testBusinesses: Option[Seq[SelfEmploymentData]] = Some(Seq(SelfEmploymentData(
    id = "12345",
    businessStartDate = Some(BusinessStartDate(DateModel("05", "04", "2017"))),
    businessName = Some(testBusinessName.encrypt(crypto.QueryParameterCrypto)),
    businessTradeName = Some(testBusinessTrade),
    businessAddress = Some(testBusinessAddress.encrypt(crypto.QueryParameterCrypto))
  )))
  private val tooManyBusinesses = 51
  val testTooManyBusinesses: Seq[SelfEmploymentData] = Array.range(1, tooManyBusinesses).map(i =>
    SelfEmploymentData(
      id = i.toString,
      businessStartDate = Some(BusinessStartDate(DateModel("05", "04", "2017"))),
      businessName = Some(BusinessNameModel(s"${testBusinessName.businessName} $i").encrypt(crypto.QueryParameterCrypto)),
      businessTradeName = Some(testBusinessTrade),
      businessAddress = Some(testBusinessAddress.encrypt(crypto.QueryParameterCrypto))
    )).toSeq
  val testFullPropertyModel: PropertyModel = PropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    count = Some(1),
    startDate = Some(testPropertyStartDate.startDate),
    confirmed = true
  )

  val testFullOverseasPropertyModel: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    count = Some(1),
    startDate = Some(testPropertyStartDateModel.startDate),
    confirmed = true
  )

  private val testBusinessTradeName: BusinessTradeNameModel = BusinessTradeNameModel("test trade name")
  private val testBusinessStartDate: BusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  private val testId = "testId"

  lazy val testUserDetails: UserDetailsModel = UserDetailsModel(testFirstName, testLastName, testNino, testOneDayAgo)

  lazy val testMTDITEnrolmentKey: EnrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMtdId)
  lazy val testIRSAEnrolmentKey: EnrolmentKey = EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr)

  lazy val testSummaryDataSelfEmploymentData: Seq[SelfEmploymentData] =
    Seq(SelfEmploymentData(
      id = testId,
      businessStartDate = Some(testBusinessStartDate),
      businessName = Some(testBusinessName.encrypt(crypto.QueryParameterCrypto)),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1", "line 2"), Some("TF2 1PF"))).encrypt(crypto.QueryParameterCrypto))
    ))
}
