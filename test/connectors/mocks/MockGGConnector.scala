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
import connectors.GGConnector
import connectors.models.gg.EnrolRequest
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.HttpPost
import utils.Implicits._

trait MockGGConnector extends MockHttp {
//
//  lazy val config: Configuration = app.injector.instanceOf[Configuration]
//  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
//  lazy val httpPost: HttpPost = mockHttpPost
//  lazy val logging: Logging = app.injector.instanceOf[Logging]
//
//  object TestGovernmentGatewayEnrolConnector extends GGConnector(config, httpPost, appConfig, logging)
//
//  def mockGovernmentGatewayEnrol(payload: EnrolRequest) = (setupMockGovernmentGatewayEnrol(payload) _).tupled
//
//  def setupMockGovernmentGatewayEnrol(payload: EnrolRequest)(status: Int, response: JsValue): Unit =
//    setupMockHttpPost(url = TestGovernmentGatewayEnrolConnector.enrolUrl, payload)(status, response)
//
//  def verifyMockGovernmentGatewayEnrol(payload: Option[EnrolRequest]=None)(count: Int): Unit =
//    verifyHttpPost(url = TestGovernmentGatewayEnrolConnector.enrolUrl, payload)(count)

}
