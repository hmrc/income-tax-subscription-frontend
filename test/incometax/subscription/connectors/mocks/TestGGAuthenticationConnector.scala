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

import connectors.mocks.MockHttp
import core.audit.Logging
import incometax.subscription.connectors.GGAuthenticationConnector
import incometax.subscription.models.{RefreshProfileFailure, RefreshProfileSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.JsNull
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}
import core.utils.MockTrait
import core.utils.TestConstants._

import scala.concurrent.Future

trait MockGGAuthenticationConnector extends MockTrait {
  val mockGGAuthenticationConnector = mock[GGAuthenticationConnector]

  private def mockRefreshProfile(result: Future[Either[RefreshProfileFailure.type, RefreshProfileSuccess.type]]): Unit =
    when(mockGGAuthenticationConnector.refreshProfile()(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockRefreshProfileSuccess(): Unit = mockRefreshProfile(Future.successful(Right(RefreshProfileSuccess)))

  def mockRefreshProfileFailure(): Unit = mockRefreshProfile(Future.successful(Left(RefreshProfileFailure)))

  def mockRefreshProfileException(): Unit = mockRefreshProfile(Future.failed(testException))
}

trait TestGGAuthenticationConnector extends MockHttp {

  lazy val logging: Logging = app.injector.instanceOf[Logging]
  lazy val httpPost: HttpPost = mockHttpPost
  lazy val httpGet: HttpGet = mockHttpGet

  def mockRefreshProfileSuccess(): Unit =
    setupMockHttpPostEmpty(Some(TestGGAuthenticationConnector.refreshProfileUrl))(Status.NO_CONTENT, JsNull)

  def mockRefreshProfileFailure(): Unit =
    setupMockHttpPostEmpty(Some(TestGGAuthenticationConnector.refreshProfileUrl))(Status.INTERNAL_SERVER_ERROR, JsNull)

  def mockRefreshProfileException(): Unit =
    setupMockHttpPostEmptyException(Some(TestGGAuthenticationConnector.refreshProfileUrl))(testException)

  object TestGGAuthenticationConnector extends GGAuthenticationConnector(appConfig, httpPost, logging)

}
