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

package core.utils

import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.UUID

import core.Constants.GovernmentGateway._
import core.Constants._
import core.models.{Cash, DateModel}
import core.utils.TestModels._
import incometax.subscription.models._
import play.api.http.Status._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.UserId
import usermatching.models.{LockedOut, UserMatchFailureResponseModel, UserMatchSuccessResponseModel}

object TestConstants {
  /*
  * this nino is a constant, if you need a fresh one use TestModels.newNino
  */
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testUtr: String = UUID.randomUUID().toString
  //Not an actual UTR
  lazy val testArn: String = UUID.randomUUID().toString
  lazy val testToken: String = s"${UUID.randomUUID()}"
  lazy val testMTDID = "XE0001234567890"
  lazy val startDate = DateModel("05", "04", "2017")
  lazy val endDate = DateModel("04", "04", "2018")
  lazy val ggServiceName = "HMRC-MTD-IT"
  lazy val testLockoutResponse = LockedOut(testNino, OffsetDateTime.now())
  lazy val testAgencyName = UUID.randomUUID().toString

  val testUserId = UserId(s"/auth/oid/1234567")
  val escapedUserId = URLEncoder.encode(testUserId.value, "UTF-8")

  val testFirstName = "Test"
  val testLastName = "Name"

  val testPhoneNumber = "000 000 0000"

  val testUrl = "/test/url/"

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
    accountingPeriodStart = Some(testAccountingPeriod.startDate),
    accountingPeriodEnd = Some(testAccountingPeriod.endDate),
    cashOrAccruals = Some(Cash),
    tradingName = testBusinessName.businessName,
    arn = None
  )

  val testAdjustedSubmissionRequest = SubscriptionRequest(
    nino = TestConstants.testNino,
    incomeSource = Both,
    accountingPeriodStart = Some(testAccountingPeriod.adjustedTaxYear.startDate),
    accountingPeriodEnd = Some(testAccountingPeriod.adjustedTaxYear.endDate),
    cashOrAccruals = Some(Cash),
    tradingName = testBusinessName.businessName,
    arn = None
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

  val testGroupId = UUID.randomUUID().toString

  val testCredId = UUID.randomUUID().toString

  val testEnrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMTDID)

  val testEnrolmentRequest = EmacEnrolmentRequest(testCredId, testNino)

}
