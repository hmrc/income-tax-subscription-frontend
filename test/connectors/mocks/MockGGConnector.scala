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
import connectors.GGConnector
import connectors.models.gg.EnrolRequest
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{JsNull, JsString}
import uk.gov.hmrc.play.http.HttpPost

trait MockGGConnector extends MockHttp {

  lazy val config: Configuration = app.injector.instanceOf[Configuration]
  lazy val httpPost: HttpPost = mockHttpPost
  lazy val logging: Logging = app.injector.instanceOf[Logging]

  val errorJson = JsString("This is an error")
  val testException = new Exception

  def mockEnrolSuccess(request: EnrolRequest): Unit =
    setupMockHttpPost(Some(TestGovernmentGatewayEnrolConnector.enrolUrl), Some(request))(Status.OK, JsNull)

  def mockEnrolFailure(request: EnrolRequest): Unit =
    setupMockHttpPost(Some(TestGovernmentGatewayEnrolConnector.enrolUrl), Some(request))(Status.INTERNAL_SERVER_ERROR, errorJson)

  def mockEnrolException(request: EnrolRequest): Unit =
    setupMockHttpPostException(Some(TestGovernmentGatewayEnrolConnector.enrolUrl), Some(request))(testException)

  object TestGovernmentGatewayEnrolConnector extends GGConnector(httpPost, appConfig, logging)

}
