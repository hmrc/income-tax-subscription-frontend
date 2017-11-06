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

package incometax.subscription.connectors.mocks

import core.connectors.mocks.MockHttp
import core.audit.Logging
import incometax.subscription.connectors.GGConnector
import incometax.subscription.models.{EnrolFailure, EnrolRequest, EnrolSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.JsNull
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import core.utils.MockTrait
import core.utils.TestConstants._

import scala.concurrent.Future

trait MockGGConnector extends MockTrait {
  val mockGGConnector = mock[GGConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGGConnector)
  }

  private def mockEnrol(request: EnrolRequest)(response: Future[Either[EnrolFailure, EnrolSuccess.type]]): Unit =
    when(mockGGConnector.enrol(ArgumentMatchers.eq(request))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockEnrolSuccess(request: EnrolRequest): Unit = mockEnrol(request)(Future.successful(Right(EnrolSuccess)))

  def mockEnrolFailure(request: EnrolRequest): Unit = mockEnrol(request)(Future.successful(Left(EnrolFailure(testErrorMessage))))

  def mockEnrolException(request: EnrolRequest): Unit = mockEnrol(request)(Future.failed(testException))

}

trait TestGGConnector extends MockHttp {

  lazy val config: Configuration = app.injector.instanceOf[Configuration]
  lazy val logging: Logging = app.injector.instanceOf[Logging]

  def mockEnrolSuccess(request: EnrolRequest): Unit =
    setupMockHttpPost(Some(TestGovernmentGatewayEnrolConnector.enrolUrl), Some(request))(Status.OK, JsNull)

  def mockEnrolFailure(request: EnrolRequest): Unit =
    setupMockHttpPost(Some(TestGovernmentGatewayEnrolConnector.enrolUrl), Some(request))(Status.INTERNAL_SERVER_ERROR, errorJson)

  def mockEnrolException(request: EnrolRequest): Unit =
    setupMockHttpPostException(Some(TestGovernmentGatewayEnrolConnector.enrolUrl), Some(request))(testException)

  object TestGovernmentGatewayEnrolConnector extends GGConnector(mockHttp, appConfig, logging)

}
