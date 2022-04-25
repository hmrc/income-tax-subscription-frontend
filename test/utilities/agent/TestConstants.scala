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

import models.DateModel
import models.common.business.{Address, BusinessAddressModel, BusinessStartDate, SelfEmploymentData}
import models.common.subscription._
import models.usermatching.LockedOut
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain.Generator
import utilities.TestModels.{testAccountMethod, testAccountingPeriod, testBusinessName, testBusinessTradeName, testValidStartDate}
import utilities.agent.Constants.{agentServiceEnrolmentName, agentServiceIdentifierKey, mtdItsaEnrolmentIdentifierKey, mtdItsaEnrolmentName}
import utilities.individual

import java.time.LocalDate
import java.util.UUID

object TestConstants {
  /*
  * this nino is a constant, if you need a fresh one use TestModels.newNino
  */
  lazy val testNino: String = individual.TestConstants.testNino
  lazy val testId: String = "testId"
  lazy val testUtr: String = individual.TestConstants.testUtr
  lazy val testMTDID: String = individual.TestConstants.testMTDID
  //Not a valid MTDID, for test purposes only
  lazy val startDate: DateModel = individual.TestConstants.startDate
  lazy val endDate: DateModel = individual.TestConstants.endDate
  lazy val ggServiceName: String = mtdItsaEnrolmentName
  lazy val agentServiceName: String = agentServiceEnrolmentName
  lazy val testARN: String = new Generator().nextAtedUtr.utr //Not a valid ARN, for test purposes only

  val testSoleTraderBusinesses = SoleTraderBusinesses(testAccountingPeriod, testAccountMethod, testSelfEmploymentData)
  val testUkProperty = UkProperty(testAccountingPeriod, testValidStartDate, testAccountMethod)
  val testOverseasProperty = OverseasProperty(testAccountingPeriod, testValidStartDate, testAccountMethod)
  lazy val businessStartDate = BusinessStartDate(DateModel("05", "04", "2017"))

  lazy val knownFactsRequest = KnownFactsRequest(
    List(
      TypeValuePair(mtdItsaEnrolmentIdentifierKey, testMTDID),
      TypeValuePair(agentServiceIdentifierKey, testNino)
    )
  )

  val testErrorMessage = "This is an error"
  val testException: Exception = individual.TestConstants.testException

  val minStartDate: LocalDate = LocalDate.of(LocalDate.now.getYear, 4, 6)

  val testCredId = UUID.randomUUID().toString

  val testSubscriptionSuccess = Right(SubscriptionSuccess(testMTDID))

  val testSubscriptionFailure = Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))

  val testSignUpIncomeSourcesFailure = Left(SignUpIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))

  val testCreateIncomeSourcesFailure = Left(CreateIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))

  val testKnownFactsSuccess = Right(KnownFactsSuccess)

  val testKnownFactsFailure = Left(KnownFactsFailure(testErrorMessage))

  val testCreateSubscriptionFromTaskListFailure = Left(CreateIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))

  lazy val testLockoutResponse: LockedOut = individual.TestConstants.testLockoutResponse

  lazy val testCreateIncomeSources: CreateIncomeSourcesModel =
    CreateIncomeSourcesModel(
      testNino,
      Some(testSoleTraderBusinesses),
      Some(testUkProperty),
      Some(testOverseasProperty)
    )

  lazy val testSelfEmploymentData: Seq[SelfEmploymentData] =
    Seq(SelfEmploymentData
    (
      id = testId,
      businessStartDate = Some(businessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), Some("TF2 1PF"))))
    )
    )

}
