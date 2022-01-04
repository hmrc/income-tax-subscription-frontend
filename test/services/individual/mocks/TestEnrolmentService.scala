/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.MockAuth
import config.MockConfig
import connectors.individual.subscription.mocks.MockTaxEnrolmentsConnector
import models.common.subscription.{EnrolFailure, EnrolRequest, EnrolSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.individual.EnrolmentService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.Constants.GovernmentGateway.{ggServiceName, _}
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait TestEnrolmentService extends MockTaxEnrolmentsConnector with MockAuth {

  object TestEnrolmentService extends EnrolmentService(MockConfig, mockTaxEnrolmentsConnector, mockAuth)

  object TestEnrolmentServiceFeatureSwitched extends EnrolmentService(
    MockConfig,
    mockTaxEnrolmentsConnector,
    mockAuth
  )

  val expectedRequestModel = EnrolRequest(
    portalId = ggPortalId,
    serviceName = ggServiceName,
    friendlyName = ggFriendlyName,
    knownFacts = List(testMTDID, testNino)
  )
}

trait MockEnrolmentService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockEnrolmentService: EnrolmentService = mock[EnrolmentService]

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
