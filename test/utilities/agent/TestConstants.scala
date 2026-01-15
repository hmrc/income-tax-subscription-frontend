/*
 * Copyright 2023 HM Revenue & Customs
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

import models.DateModel
import models.common.business.{Address, BusinessAddressModel, BusinessStartDate, SelfEmploymentData}
import models.common.subscription.*
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain.*
import utilities.TestModels.*
import utilities.individual

import java.util.UUID

object TestConstants {
  lazy val testNino: String = individual.TestConstants.testNino

  lazy val testId: String = "testId"
  lazy val testUtr: String = individual.TestConstants.testUtr
  lazy val testMTDID: String = individual.TestConstants.testMTDID
  //Not a valid MTDID, for test purposes only
  lazy val testARN: String = new AtedUtrGenerator().nextAtedUtr.utr //Not a valid ARN, for test purposes only

  val testSoleTraderBusinessesThisYear = SoleTraderBusinesses(testAccountingPeriodThisYear, testSelfEmploymentData)
  val testUkPropertyThisYear = UkProperty(startDateBeforeLimit = None, testAccountingPeriodThisYear, testValidStartDate)
  val testUkPropertyNextYear = UkProperty(startDateBeforeLimit = None, testAccountingPeriodNextYear, testValidStartDate)
  val testOverseasPropertyThisYear = OverseasProperty(startDateBeforeLimit = None, testAccountingPeriodThisYear, testValidStartDate)
  val testOverseasPropertyNextYear = OverseasProperty(startDateBeforeLimit = None, testAccountingPeriodNextYear, testValidStartDate)
  lazy val businessStartDate = BusinessStartDate(DateModel("05", "04", "2017"))

  val testErrorMessage = "This is an error"
  val testException: Exception = individual.TestConstants.testException

  val testCredId = UUID.randomUUID().toString

  val testSubscriptionSuccess = Right(Some(SubscriptionSuccess(testMTDID, None)))

  val testSubscriptionFailure = Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))

  lazy val testCreateIncomeSourcesThisYear: CreateIncomeSourcesModel =
    CreateIncomeSourcesModel(
      testNino,
      Some(testSoleTraderBusinessesThisYear),
      Some(testUkPropertyThisYear),
      Some(testOverseasPropertyThisYear)
    )

  lazy val testSelfEmploymentData: Seq[SelfEmploymentData] =
    Seq(SelfEmploymentData(
      id = testId,
      businessStartDate = Some(businessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1", "line 2"), Some("TF2 1PF"))))
    ))

}
