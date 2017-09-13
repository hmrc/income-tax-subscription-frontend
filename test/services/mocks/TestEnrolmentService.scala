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

import common.Constants.GovernmentGateway.{ggServiceName, _}
import connectors.mocks.MockGGConnector
import connectors.models.gg._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.EnrolmentService
import utils.MockTrait
import utils.TestConstants._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

trait TestEnrolmentService extends MockGGConnector {

  object TestEnrolmentService extends EnrolmentService(mockGGConnector)

  val expectedRequestModel = EnrolRequest(
    portalId = ggPortalId,
    serviceName = ggServiceName,
    friendlyName = ggFriendlyName,
    knownFacts = List(testMTDID, testNino)
  )
}

trait MockEnrolmentService extends MockTrait {
  val mockEnrolmentService = mock[EnrolmentService]

  private def mockEnrol(mtditid: String, nino: String)(response: Future[Either[EnrolFailure, EnrolSuccess.type]]): Unit =
    when(
      mockEnrolmentService.enrol(
        ArgumentMatchers.eq(mtditid),
        ArgumentMatchers.eq(nino)
      )(
        ArgumentMatchers.any[HeaderCarrier],
        ArgumentMatchers.any[ExecutionContext]
      )
    ).thenReturn(response)

  def mockEnrolSuccess(mtditid: String, nino: String): Unit = {
    mockEnrol(mtditid, nino)(Future.successful(testEnrolSuccess))
  }
  def mockEnrolFailure(mtditid: String, nino: String): Unit = {
    mockEnrol(mtditid, nino)(Future.successful(testEnrolFailure))
  }

  def mockEnrolException(mtditid: String, nino: String): Unit =
    mockEnrol(mtditid, nino)(Future.failed(testException))
}
