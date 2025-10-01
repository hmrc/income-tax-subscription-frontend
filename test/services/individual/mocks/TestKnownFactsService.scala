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

package services.individual.mocks

import common.Constants.GovernmentGateway._
import config.MockConfig
import connectors.individual.subscription.mocks.MockTaxEnrolmentsConnector
import models.common.subscription.{KnownFactsFailure, KnownFactsRequest, KnownFactsSuccess, TypeValuePair}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.individual.KnownFactsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait TestKnownFactsService extends MockTaxEnrolmentsConnector with MockConfig {

  object TestKnownFactsService extends KnownFactsService(
    mockTaxEnrolmentsConnector,
    MockConfig
  )

  val expectedRequestModel = KnownFactsRequest(List(
    TypeValuePair(MTDITID, testMTDID),
    TypeValuePair(NINO, testNino)
  ))
}

trait MockKnownFactsService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {
  val mockKnownFactsService = mock[KnownFactsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockKnownFactsService)
  }

  private def mockAddKnownFacts(mtditid: String, nino: String)(response: Future[Either[KnownFactsFailure, KnownFactsSuccess.type]]): Unit =
    when(mockKnownFactsService.addKnownFacts(ArgumentMatchers.eq(mtditid), ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def verifyAddKnownFacts(mtditid: String, nino: String, count: Int = 1): Unit = {
    verify(mockKnownFactsService, times(count))
      .addKnownFacts(ArgumentMatchers.eq(mtditid), ArgumentMatchers.eq(nino))(ArgumentMatchers.any())
  }

  def mockAddKnownFactsSuccess(mtditid: String, nino: String): Unit =
    mockAddKnownFacts(mtditid, nino)(Future.successful(testKnownFactsSuccess))

  def mockAddKnownFactsFailure(mtditid: String, nino: String): Unit =
    mockAddKnownFacts(mtditid, nino)(Future.successful(testKnownFactsFailure))

  def mockAddKnownFactsException(mtditid: String, nino: String): Unit =
    mockAddKnownFacts(mtditid, nino)(Future.failed(testException))
}
