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
import connectors.iv.IdentityVerificationConnector
import connectors.models.iv.{IVFailure, IVJourneyResult, IVSuccess, IVTimeout}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.{MockTrait, UnitTestTrait}

import scala.concurrent.Future

trait TestIdentityVerificationConnector extends UnitTestTrait
  with MockHttp {

  object TestIdentityVerificationConnector extends IdentityVerificationConnector(
    app.injector.instanceOf[AppConfig],
    mockHttpGet,
    app.injector.instanceOf[Logging]
  )

  private def responseJson(code: String): JsValue =
    Json.parse(s"""{ "result": "$code", "token": "123-456-dfg-gg-rrrr-3"}""")

  def mockIVSuccess(journeyId: String): Unit =
    setupMockHttpGet(TestIdentityVerificationConnector.journeyResultUrl(journeyId))(OK, responseJson("Success"))

  def mockIVTimeout(journeyId: String): Unit =
    setupMockHttpGet(TestIdentityVerificationConnector.journeyResultUrl(journeyId))(OK, responseJson("Timeout"))

  def mockIVFailure(journeyId: String): Unit =
    setupMockHttpGet(TestIdentityVerificationConnector.journeyResultUrl(journeyId))(OK, responseJson("TechnicalIssue"))

  def mockIVNotFound(journeyId: String): Unit =
    setupMockHttpGet(TestIdentityVerificationConnector.journeyResultUrl(journeyId))(NOT_FOUND, responseJson(""))

  def mockIVException(journeyId: String): Unit =
    setupMockHttpGet(TestIdentityVerificationConnector.journeyResultUrl(journeyId))(INTERNAL_SERVER_ERROR, responseJson(""))

}


trait MockIdentityVerificationConnector extends UnitTestTrait
  with MockTrait {

  val mockIdentityVerificationConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIdentityVerificationConnector)
  }

  private def mockJourneyResult(journeyId: String)(response: Future[IVJourneyResult]): Unit =
    when(mockIdentityVerificationConnector.getJourneyResult(ArgumentMatchers.contains(journeyId))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  val testException = new InternalServerException("future failed")

  def mockIVSuccess(journeyId: String): Unit = mockJourneyResult(journeyId)(IVSuccess)

  def mockIVTimeout(journeyId: String): Unit = mockJourneyResult(journeyId)(IVTimeout)

  def mockIVFailure(journeyId: String): Unit = mockJourneyResult(journeyId)(IVFailure)

  def mockIVException(journeyId: String): Unit =
    mockJourneyResult(journeyId)(Future.failed(testException))

}