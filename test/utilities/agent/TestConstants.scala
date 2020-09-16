/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate
import java.util.UUID

import models.DateModel
import models.individual.subscription._
import models.usermatching.LockedOut
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain.Generator
import utilities.agent.Constants.{agentServiceEnrolmentName, agentServiceIdentifierKey, mtdItsaEnrolmentIdentifierKey, mtdItsaEnrolmentName}
import utilities.individual

object TestConstants {
  /*
  * this nino is a constant, if you need a fresh one use TestModels.newNino
  */
  lazy val testNino: String = individual.TestConstants.testNino
  lazy val testUtr: String = individual.TestConstants.testUtr
  lazy val testMTDID: String = individual.TestConstants.testMTDID
  //Not a valid MTDID, for test purposes only
  lazy val startDate: DateModel = individual.TestConstants.startDate
  lazy val endDate: DateModel = individual.TestConstants.endDate
  lazy val ggServiceName: String = mtdItsaEnrolmentName
  lazy val agentServiceName: String = agentServiceEnrolmentName
  lazy val testARN: String = new Generator().nextAtedUtr.utr //Not a valid ARN, for test purposes only

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

  val testKnownFactsSuccess = Right(KnownFactsSuccess)

  val testKnownFactsFailure = Left(KnownFactsFailure(testErrorMessage))

  lazy val testLockoutResponse: LockedOut = individual.TestConstants.testLockoutResponse

}
