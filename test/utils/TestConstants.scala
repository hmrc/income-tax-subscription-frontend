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

import common.Constants.GovernmentGateway._
import connectors.models.authenticator.{RefreshProfileFailure, RefreshProfileSuccess}
import connectors.models.gg._
import connectors.models.subscription.SubscriptionResponse.{SubscriptionFailureResponse, SubscriptionSuccess}
import connectors.models.subscription.{Both, SubscriptionRequest}
import models.DateModel
import play.api.http.Status._
import uk.gov.hmrc.domain.Generator

object TestConstants {
  /*
  * this nino is a constant, if you need a fresh one use TestModels.newNino
  */
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testMTDID = "XE0001234567890"
  lazy val startDate = DateModel("05", "04", "2017")
  lazy val endDate = DateModel("04", "04", "2018")
  lazy val ggServiceName = "HMRC-MTD-IT"

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
}
