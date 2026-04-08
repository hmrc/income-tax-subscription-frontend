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
import models.common.subscription.*
import models.usermatching.LockedOut
import uk.gov.hmrc.domain.*

import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.UUID

object TestConstants {

  lazy val testNino: String = new NinoGenerator().nextNino.nino
  lazy val testUtr: String = UUID.randomUUID().toString
  lazy val testFullName: String = UUID.randomUUID().toString + " " + UUID.randomUUID().toString
  lazy val testMTDID = "XE0001234567890"
  lazy val testLockoutResponse: LockedOut = LockedOut(testNino, OffsetDateTime.now())
  lazy val testSpsEntityId: String = UUID.randomUUID().toString

  val testUserId = "/auth/oid/1234567"
  val escapedUserId: String = URLEncoder.encode(testUserId, "UTF-8")

  val testFirstName = "Test"
  val testLastName = "Name"

  val testCredentialId: String = UUID.randomUUID().toString
  val testCredentialId2: String = UUID.randomUUID().toString

  val testErrorMessage = "This is an error"
  val testException = new Exception

  val testEnrolSuccess = Right(EnrolSuccess)

  val testEnrolFailure = Left(EnrolFailure(testErrorMessage))

  val testKnownFactsSuccess = Right(KnownFactsSuccess)

  val testKnownFactsFailure = Left(KnownFactsFailure(testErrorMessage))

  val testGroupId: String = UUID.randomUUID().toString

  val testCredId: String = UUID.randomUUID().toString

  val testEnrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, MTDITID -> testMTDID)

  val testEnrolmentRequest = EmacEnrolmentRequest(testCredId, testNino)

}
