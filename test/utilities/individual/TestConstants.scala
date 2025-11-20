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

package utilities.individual

import common.Constants.GovernmentGateway.MTDITID
import common.Constants.mtdItsaEnrolmentName
import connectors.individual.httpparsers.AllocateEnrolmentResponseHttpParser.{EnrolFailure, EnrolSuccess}
import connectors.individual.httpparsers.UpsertEnrolmentResponseHttpParser.{KnownFactsFailure, KnownFactsSuccess}
import models.DateModel
import models.common.business._
import models.common.subscription.SignUpSuccessResponse.{AlreadySignedUp, SignUpSuccessful}
import models.common.subscription._
import models.usermatching.{LockedOut, UserMatchFailureResponseModel, UserMatchSuccessResponseModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain._
import utilities.TestModels._

import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.UUID

object TestConstants {

  lazy val testNino: String = new NinoGenerator().nextNino.nino
  lazy val testId: String = "testId"
  lazy val testUtr: String = UUID.randomUUID().toString
  lazy val testFullName: String = UUID.randomUUID().toString + " " + UUID.randomUUID().toString
  lazy val testMTDID = "XE0001234567890"
  lazy val businessStartDate: BusinessStartDate = BusinessStartDate(DateModel("05", "04", "2017"))
  lazy val testLockoutResponse: LockedOut = LockedOut(testNino, OffsetDateTime.now())
  lazy val testSpsEntityId: String = UUID.randomUUID().toString

  val testUserId = "/auth/oid/1234567"
  val escapedUserId: String = URLEncoder.encode(testUserId, "UTF-8")

  val testFirstName = "Test"
  val testLastName = "Name"

  val testSoleTraderBusinesses: SoleTraderBusinesses = SoleTraderBusinesses(testAccountingPeriodThisYear, testSelfEmploymentData)
  val testUkProperty: UkProperty = UkProperty(startDateBeforeLimit = None, testAccountingPeriodThisYear, testValidStartDate)
  val testOverseasProperty: OverseasProperty = OverseasProperty(startDateBeforeLimit = None, testAccountingPeriodThisYear, testValidStartDate)

  val testCredentialId: String = UUID.randomUUID().toString
  val testCredentialId2: String = UUID.randomUUID().toString

  val testErrorMessage = "This is an error"
  val testException = new Exception

  val testEnrolSuccess = Right(EnrolSuccess)

  val testEnrolFailure = Left(EnrolFailure(testErrorMessage))

  val testSubscriptionSuccess = Right(Some(SubscriptionSuccess(testMTDID)))

  val testSubscriptionFailure = Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))

  val testKnownFactsSuccess = Right(KnownFactsSuccess)

  val testKnownFactsFailure = Left(KnownFactsFailure(testErrorMessage))

  val testMatchSuccess = Right(UserMatchSuccessResponseModel)

  val testMatchFailure = Left(UserMatchFailureResponseModel)

  val testGroupId: String = UUID.randomUUID().toString

  val testCredId: String = UUID.randomUUID().toString

  val testEnrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMTDID)

  val testEnrolmentRequest = EmacEnrolmentRequest(testCredId, testNino)

  val testSignUpIncomeSourcesSuccess = Right(SignUpSuccessful(testMTDID))

  val testAlreadySignUpIncomeSources = Right(AlreadySignedUp)

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

  lazy val testCreateIncomeSources: CreateIncomeSourcesModel =
    CreateIncomeSourcesModel(
      testNino,
      Some(testSoleTraderBusinesses),
      Some(testUkProperty),
      Some(testOverseasProperty)
    )

}
