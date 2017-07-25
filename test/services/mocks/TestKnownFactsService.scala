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

package services.mocks

import common.Constants.GovernmentGateway._
import connectors.mocks.MockGGAdminConnector
import connectors.models.gg._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.KnownFactsService
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.MockTrait
import utils.TestConstants._

import scala.concurrent.Future

trait TestKnownFactsService extends MockGGAdminConnector {

  object TestKnownFactsService extends KnownFactsService(mockGGAdminConnector)

  val expectedRequestModel = KnownFactsRequest(List(
    TypeValuePair(MTDITID, testMTDID),
    TypeValuePair(NINO, testNino)
  ))
}

trait MockKnownFactsService extends MockTrait {
  val mockKnownFactsService = mock[KnownFactsService]

  private def mockAddKnownFacts(mtditid: String, nino: String)(response: Future[Either[KnownFactsFailure, KnownFactsSuccess.type]]): Unit =
    when(mockKnownFactsService.addKnownFacts(ArgumentMatchers.eq(mtditid), ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockAddKnownFactsSuccess(mtditid: String, nino: String): Unit =
    mockAddKnownFacts(mtditid, nino)(Future.successful(testKnownFactsSuccess))

  def mockAddKnownFactsFailure(mtditid: String, nino: String): Unit =
    mockAddKnownFacts(mtditid, nino)(Future.successful(testKnownFactsFailure))

  def mockAddKnownFactsException(mtditid: String, nino: String): Unit =
    mockAddKnownFacts(mtditid, nino)(Future.failed(testException))
}
