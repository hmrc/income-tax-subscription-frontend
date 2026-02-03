/*
 * Copyright 2023 HM Revenue & Customs
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

import auth.MockHttp
import config.AppConfig
import connectors.SubscriptionConnector
import connectors.httpparser.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import models.common.subscription.{SubscriptionFailureResponse, SubscriptionSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait MockSubscriptionConnector extends UnitTestTrait with MockitoSugar {
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  def setupMockGetSubscription(nino: String)(result: Future[GetSubscriptionResponse]): Unit =
    when(mockSubscriptionConnector.getSubscription(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockGetSubscriptionFound(nino: String): Unit =
    setupMockGetSubscription(nino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDID, None)))))

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

}
