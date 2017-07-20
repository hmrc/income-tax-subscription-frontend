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
import connectors.GGAuthenticationConnector
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.HttpPost
import utils.Implicits._

trait MockAuthenticatorConnector extends MockHttp {

//  lazy val config: Configuration = app.injector.instanceOf[Configuration]
//  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
//  lazy val logging: Logging = app.injector.instanceOf[Logging]
//  lazy val httpPost: HttpPost = mockHttpPost
//
//  val mockRefreshProfile = (setupRefreshProfile _).tupled
//
//  object TestGGAuthenticationConnector extends GGAuthenticationConnector(config, appConfig, logging, httpPost)
//
//  def setupRefreshProfile(status: Int, response: Option[JsValue]): Unit =
//    setupMockHttpPostEmpty(url = TestGGAuthenticationConnector.refreshProfileUrl)(status, response)
//
//  def verifyRefreshProfile(count: Int): Unit =
//    verifyMockHttpPostEmpty(url = TestGGAuthenticationConnector.refreshProfileUrl)(count)

}
