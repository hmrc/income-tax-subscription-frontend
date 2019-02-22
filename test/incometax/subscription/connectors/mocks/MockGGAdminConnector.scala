/*
 * Copyright 2019 HM Revenue & Customs
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
import incometax.subscription.connectors.GGAdminConnector
import incometax.subscription.models.{KnownFactsFailure, KnownFactsRequest, KnownFactsSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}
import core.utils.MockTrait
import core.utils.TestConstants._

import scala.concurrent.Future

trait MockGGAdminConnector extends MockTrait {
  val mockGGAdminConnector = mock[GGAdminConnector]

  private def mockAddKnownFacts(request: KnownFactsRequest)(response: Future[Either[KnownFactsFailure, KnownFactsSuccess.type]]): Unit =
    when(mockGGAdminConnector.addKnownFacts(ArgumentMatchers.eq(request))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockAddKnownFactsSuccess(request: KnownFactsRequest): Unit =
    mockAddKnownFacts(request)(Future.successful(Right(KnownFactsSuccess)))

  def mockAddKnownFactsFailure(request: KnownFactsRequest): Unit =
    mockAddKnownFacts(request)(Future.successful(Left(KnownFactsFailure(testErrorMessage))))

  def mockAddKnownFactsException(request: KnownFactsRequest): Unit =
    mockAddKnownFacts(request)(Future.failed(testException))

}

trait TestGGAdminConnector extends MockHttp {
  lazy val logging: Logging = app.injector.instanceOf[Logging]

  def mockAddKnownFactsSuccess(request: KnownFactsRequest): Unit =
    setupMockHttpPost(Some(TestGGAdminConnector.addKnownFactsUrl), Some(request))(Status.OK, JsNull)

  def mockAddKnownFactsFailure(request: KnownFactsRequest): Unit =
    setupMockHttpPost(Some(TestGGAdminConnector.addKnownFactsUrl), Some(request))(Status.INTERNAL_SERVER_ERROR, errorJson)

  def mockAddKnownFactsException(request: KnownFactsRequest): Unit =
    setupMockHttpPostException(Some(TestGGAdminConnector.addKnownFactsUrl), Some(request))(testException)

  object TestGGAdminConnector extends GGAdminConnector(appConfig, mockHttp, logging)

}
