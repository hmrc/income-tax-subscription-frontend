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

package incometax.subscription.services.mocks

import core.Constants.GovernmentGateway.{ggServiceName, _}
import core.config.MockConfig
import core.connectors.mocks.MockAuth
import incometax.subscription.connectors.mocks.{MockEnrolmentStoreConnector, MockGGConnector}
import incometax.subscription.models.{EnrolFailure, EnrolRequest, EnrolSuccess}
import incometax.subscription.services.EnrolmentService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import core.utils.MockTrait
import core.utils.TestConstants._

import scala.concurrent.{ExecutionContext, Future}

trait TestEnrolmentService extends MockGGConnector with MockEnrolmentStoreConnector with MockAuth {

  object TestEnrolmentService extends EnrolmentService(MockConfig, mockGGConnector, mockEnrolmentStoreConnector, mockAuth)

  object TestEnrolmentServiceFeatureSwitched extends EnrolmentService(
    new MockConfig {
      override val emacEs8ApiEnabled = true
    },
    mockGGConnector,
    mockEnrolmentStoreConnector,
    mockAuth
  )

  val expectedRequestModel = EnrolRequest(
    portalId = ggPortalId,
    serviceName = ggServiceName,
    friendlyName = ggFriendlyName,
    knownFacts = List(testMTDID, testNino)
  )
}

trait MockEnrolmentService extends MockTrait {
  val mockEnrolmentService = mock[EnrolmentService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEnrolmentService)
  }

  private def mockEnrol(mtditid: String, nino: String)(response: Future[Either[EnrolFailure, EnrolSuccess.type]]): Unit =
    when(
      mockEnrolmentService.enrol(
        ArgumentMatchers.eq(mtditid),
        ArgumentMatchers.eq(nino)
      )(
        ArgumentMatchers.any[HeaderCarrier]
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
