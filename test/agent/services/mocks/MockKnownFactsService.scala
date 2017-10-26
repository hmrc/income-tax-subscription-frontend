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

package agent.services.mocks

import agent.common.Constants._
import agent.connectors.mocks.MockGGAdminConnector
import agent.connectors.models.gg._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import agent.services.KnownFactsService
import agent.utils.MockTrait
import agent.utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait TestKnownFactsService extends MockGGAdminConnector {

  object TestKnownFactsService extends KnownFactsService(mockGGAdminConnector)

  val expectedRequestModel = KnownFactsRequest(List(
    TypeValuePair(mtdItsaEnrolmentIdentifierKey, testMTDID),
    TypeValuePair(ninoIdentifierKey, testNino)
  ))
}

trait MockKnownFactsService extends MockTrait {
  val mockKnownFactsService: KnownFactsService = mock[KnownFactsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockKnownFactsService)
  }

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
