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

import java.time.OffsetDateTime
import java.util.UUID

import common.Constants.GovernmentGateway._
import connectors.models.authenticator.{RefreshProfileFailure, RefreshProfileSuccess}
import connectors.models.gg._
import connectors.models.matching.{LockedOut, UserMatchFailureResponseModel, UserMatchSuccessResponseModel}
import connectors.models.subscription.{Both, SubscriptionFailureResponse, SubscriptionRequest, SubscriptionSuccess}
import models.DateModel
import play.api.http.Status._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.UserId

object TestConstants {
  /*
  * this nino is a constant, if you need a fresh one use TestModels.newNino
  */
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testUtr: String = UUID.randomUUID().toString
  //Not an actual UTR
  lazy val testToken: String = "token"
  lazy val testMTDID = "XE0001234567890"
  lazy val startDate = DateModel("05", "04", "2017")
  lazy val endDate = DateModel("04", "04", "2018")
  lazy val ggServiceName = "HMRC-MTD-IT"
  lazy val testLockoutResponse = LockedOut(testNino, OffsetDateTime.now())

  val strippedUserId = "1234567"
  val testUserId = UserId(s"/auth/oid/$strippedUserId")

  val testFirstName = "Test"
  val testLastName = "Name"

  val testPhoneNumber = "000 000 0000"

  lazy val testPaperlessPreferenceToken = s"${UUID.randomUUID()}"

  val testUrl = "/"

  lazy val knownFactsRequest = KnownFactsRequest(
    List(
      TypeValuePair(MTDITID, testMTDID),
      TypeValuePair(NINO, testNino)
    )
  )

  val testEnrolRequest = EnrolRequest(
    portalId = ggPortalId,
    serviceName = ggServiceName,
    friendlyName = ggFriendlyName,
    knownFacts = List(testMTDID, testNino)
  )

  val testSubmissionRequest = SubscriptionRequest(
    nino = TestConstants.testNino,
    incomeSource = Both,
    accountingPeriodStart = Some(startDate),
    accountingPeriodEnd = Some(endDate),
    cashOrAccruals = Some("Cash"),
    tradingName = Some("ABC")
  )

  val testErrorMessage = "This is an error"
  val testException = new Exception

  val testRefreshProfileSuccess = Right(RefreshProfileSuccess)

  val testRefreshProfileFailure = Left(RefreshProfileFailure)

  val testEnrolSuccess = Right(EnrolSuccess)

  val testEnrolFailure = Left(EnrolFailure(testErrorMessage))

  val testSubscriptionSuccess = Right(SubscriptionSuccess(testMTDID))

  val testSubscriptionFailure = Left(SubscriptionFailureResponse(INTERNAL_SERVER_ERROR))

  val testKnownFactsSuccess = Right(KnownFactsSuccess)

  val testKnownFactsFailure = Left(KnownFactsFailure(testErrorMessage))

  val testMatchSuccess = Right(UserMatchSuccessResponseModel)

  val testMatchFailure = Left(UserMatchFailureResponseModel)

}
