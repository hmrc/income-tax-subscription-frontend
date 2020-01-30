/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors.individual.subscription.mocks

import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import connectors.individual.subscription.{SubscriptionConnector, SubscriptionConnectorV2}
import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import core.config.AppConfig
import core.connectors.mocks.MockHttp
import core.utils.MockTrait
import core.utils.TestConstants._
import incometax.subscription.models.{BadlyFormattedSubscriptionResponse, SubscriptionFailureResponse, SubscriptionRequestV2, SubscriptionSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubscriptionConnector extends MockTrait {
  val mockSubscriptionConnector = mock[SubscriptionConnector]

  private def setupMockSubscribe(request: SubscriptionRequestV2)(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionConnector.subscribe(ArgumentMatchers.eq(request))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockSubscribeSuccess(request: SubscriptionRequestV2): Unit =
    setupMockSubscribe(request)(Future.successful(Right(SubscriptionSuccess(testMTDID))))

  def setupMockSubscribeFailure(request: SubscriptionRequestV2): Unit =
    setupMockSubscribe(request)(Future.successful(Left(SubscriptionFailureResponse(BAD_REQUEST))))

  def setupMockSubscribeBadFormatting(request: SubscriptionRequestV2): Unit =
    setupMockSubscribe(request)(Future.successful(Left(BadlyFormattedSubscriptionResponse)))

  def setupMockSubscribeException(request: SubscriptionRequestV2): Unit =
    setupMockSubscribe(request)(Future.failed(testException))

  def setupMockSubscriptionV2(request: SubscriptionRequestV2)(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionConnector.subscribe(ArgumentMatchers.eq(request))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)


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
    mockHttp
  )

  def setupMockSubscribe(request: SubscriptionRequestV2)(status: Int, response: JsValue): Unit =
    setupMockHttpPost[SubscriptionRequestV2](
      url = Some(TestSubscriptionConnector.subscriptionUrl("")),
      body = Some(request)
    )(status, response)

  def setupMockSubscribeSuccess(request: SubscriptionRequestV2): Unit = setupMockSubscribe(request)(OK, Json.toJson(SubscriptionSuccess(testMTDID)))

  def setupMockSubscribeEmptyBody(request: SubscriptionRequestV2): Unit = setupMockSubscribe(request)(OK, Json.obj())

  def setupMockSubscribeBadRequest(request: SubscriptionRequestV2): Unit = setupMockSubscribe(request)(BAD_REQUEST, Json.obj())

  def setupMockGetSubscription(nino: String)(status: Int, response: JsValue): Unit =
    setupMockHttpGet(
      url = TestSubscriptionConnector.subscriptionUrl(nino)
    )(status, response)

  def setupMockGetSubscriptionSuccess(nino: String): Unit = setupMockGetSubscription(nino)(OK, Json.toJson(SubscriptionSuccess(testMTDID)))

  def setupMockGetSubscriptionEmptyBody(nino: String): Unit = setupMockGetSubscription(nino)(OK, Json.obj())

  def setupMockGetSubscriptionBadRequest(nino: String): Unit = setupMockGetSubscription(nino)(BAD_REQUEST, Json.obj())
}
