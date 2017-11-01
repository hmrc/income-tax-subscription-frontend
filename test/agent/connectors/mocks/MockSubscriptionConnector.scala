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

package agent.connectors.mocks

import core.config.AppConfig
import agent.connectors.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import agent.connectors.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import agent.connectors.models.subscription._
import agent.connectors.subscription.SubscriptionConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import core.utils.JsonUtils._
import core.utils.MockTrait
import agent.utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait MockSubscriptionConnector extends MockTrait {
  val mockSubscriptionConnector = mock[SubscriptionConnector]

  private def setupMockSubscribe(request: SubscriptionRequest)(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionConnector.subscribe(ArgumentMatchers.eq(request))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockSubscribeSuccess(request: SubscriptionRequest): Unit =
    setupMockSubscribe(request)(Future.successful(Right(SubscriptionSuccess(testMTDID))))

  def setupMockSubscribeFailure(request: SubscriptionRequest): Unit =
    setupMockSubscribe(request)(Future.successful(Left(SubscriptionFailureResponse(BAD_REQUEST))))

  def setupMockSubscribeBadFormatting(request: SubscriptionRequest): Unit =
    setupMockSubscribe(request)(Future.successful(Left(BadlyFormattedSubscriptionResponse)))

  def setupMockSubscribeException(request: SubscriptionRequest): Unit =
    setupMockSubscribe(request)(Future.failed(testException))


  private def setupMockGetSubscription(nino: String)(result: Future[GetSubscriptionResponse]): Unit =
    when(mockSubscriptionConnector.getSubscription(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockGetSubscriptionFound(nino: String): Unit =
    setupMockGetSubscription(nino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDID)))))

  def setupMockGetSubscriptionNotFound(nino: String): Unit =
    setupMockGetSubscription(nino)(Future.successful(Right(None)))

  def setupMockGetSubscriptionFailure(nino: String): Unit =
    setupMockGetSubscription(nino)(Future.successful(Left(SubscriptionFailureResponse(BAD_REQUEST))))

  def setupMockGetSubscriptionException(nino: String): Unit =
    setupMockGetSubscription(nino)(Future.failed(testException))
}

trait TestSubscriptionConnector extends MockHttp {

  object TestSubscriptionConnector extends SubscriptionConnector(
    app.injector.instanceOf[AppConfig],
    mockHttpPost,
    mockHttpGet
  )

  def setupMockSubscribe(request: SubscriptionRequest)(status: Int, response: JsValue): Unit =
    setupMockHttpPost[SubscriptionRequest](
      url = Some(TestSubscriptionConnector.subscriptionUrl("")),
      body = Some(request)
    )(status, response)

  def setupMockSubscribeSuccess(request: SubscriptionRequest): Unit = setupMockSubscribe(request)(OK, SubscriptionSuccess(testMTDID))

  def setupMockSubscribeEmptyBody(request: SubscriptionRequest): Unit = setupMockSubscribe(request)(OK, Json.obj())

  def setupMockSubscribeBadRequest(request: SubscriptionRequest): Unit = setupMockSubscribe(request)(BAD_REQUEST, Json.obj())

  def setupMockGetSubscription(nino: String)(status: Int, response: JsValue): Unit =
    setupMockHttpGet(
      url = TestSubscriptionConnector.subscriptionUrl(nino)
    )(status, response)

  def setupMockGetSubscriptionSuccess(nino: String): Unit = setupMockGetSubscription(nino)(OK, SubscriptionSuccess(testMTDID))

  def setupMockGetSubscriptionEmptyBody(nino: String): Unit = setupMockGetSubscription(nino)(OK, Json.obj())

  def setupMockGetSubscriptionBadRequest(nino: String): Unit = setupMockGetSubscription(nino)(BAD_REQUEST, Json.obj())
}
