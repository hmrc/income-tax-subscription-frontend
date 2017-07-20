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

package connectors.mocks

import audit.Logging
import config.AppConfig
import connectors.{GGAdminConnector, GGAuthenticationConnector}
import connectors.models.gg.KnownFactsRequest
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{JsNull, JsString, JsValue}
import uk.gov.hmrc.play.http.{HttpGet, HttpPost}
import utils.Implicits._

trait MockAuthenticatorConnector extends MockHttp {

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
