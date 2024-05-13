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

import common.Constants.{agentServiceIdentifierKey, hmrcAsAgent, mtdItsaEnrolmentIdentifierKey, mtdItsaEnrolmentName}
import models.DateModel
import models.common.business.{Address, BusinessAddressModel, BusinessStartDate, SelfEmploymentData}
import models.common.subscription.SignUpSourcesFailure.SignUpIncomeSourcesFailureResponse
import models.common.subscription._
import models.usermatching.LockedOut
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain.Generator
import utilities.TestModels._
import utilities.individual

import java.time.LocalDate
import java.util.UUID
import scala.util.matching.Regex

object TestConstants {
  /*
  * this nino is a constant, if you need a fresh one use TestModels.newNino
  */

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  lazy val testName: String = "FirstName LastName"
  lazy val testNino: String = individual.TestConstants.testNino
  lazy val testFormattedNino: String = testNino match {
    case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
      s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
    case other => other
  }
  lazy val testId: String = "testId"
  lazy val testUtr: String = individual.TestConstants.testUtr
  lazy val testMTDID: String = individual.TestConstants.testMTDID
  //Not a valid MTDID, for test purposes only
  lazy val startDate: DateModel = individual.TestConstants.startDate
  lazy val endDate: DateModel = individual.TestConstants.endDate
  lazy val ggServiceName: String = mtdItsaEnrolmentName
  lazy val agentServiceName: String = hmrcAsAgent
  lazy val testARN: String = new Generator().nextAtedUtr.utr //Not a valid ARN, for test purposes only

  val testSoleTraderBusinessesThisYear = SoleTraderBusinesses(testAccountingPeriodThisYear, testAccountMethod, testSelfEmploymentData)
  val testSoleTraderBusinessesNextYear = SoleTraderBusinesses(testAccountingPeriodNextYear, testAccountMethod, testSelfEmploymentData)
  val testUkPropertyThisYear = UkProperty(testAccountingPeriodThisYear, testValidStartDate, testAccountMethod)
  val testUkPropertyNextYear = UkProperty(testAccountingPeriodNextYear, testValidStartDate, testAccountMethod)
  val testOverseasPropertyThisYear = OverseasProperty(testAccountingPeriodThisYear, testValidStartDate, testAccountMethod)
  val testOverseasPropertyNextYear = OverseasProperty(testAccountingPeriodNextYear, testValidStartDate, testAccountMethod)
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

  val testSubscriptionSuccess = Right(Some(SubscriptionSuccess(testMTDID)))

  val testSubscriptionFailure = Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))

  val testSignUpIncomeSourcesFailure = Left(SignUpIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))

  val testCreateIncomeSourcesFailure = Left(CreateIncomeSourcesFailure(INTERNAL_SERVER_ERROR))

  val testKnownFactsSuccess = Right(KnownFactsSuccess)

  val testKnownFactsFailure = Left(KnownFactsFailure(testErrorMessage))

  val testCreateSubscriptionFromTaskListFailure = Left(CreateIncomeSourcesFailure(INTERNAL_SERVER_ERROR))

  lazy val testLockoutResponse: LockedOut = individual.TestConstants.testLockoutResponse

  lazy val testCreateIncomeSourcesThisYear: CreateIncomeSourcesModel =
    CreateIncomeSourcesModel(
      testNino,
      Some(testSoleTraderBusinessesThisYear),
      Some(testUkPropertyThisYear),
      Some(testOverseasPropertyThisYear)
    )

  lazy val testCreateIncomeSourcesNextYear: CreateIncomeSourcesModel =
    CreateIncomeSourcesModel(
      testNino,
      Some(testSoleTraderBusinessesThisYear),
      Some(testUkPropertyNextYear),
      Some(testOverseasPropertyNextYear)
    )

  lazy val testSelfEmploymentData: Seq[SelfEmploymentData] =
    Seq(SelfEmploymentData
    (
      id = testId,
      businessStartDate = Some(businessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1", "line 2"), Some("TF2 1PF"))))
    )
    )

}
