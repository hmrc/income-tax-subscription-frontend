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

package incometax.subscription.services.mocks

import core.audit.Logging
import incometax.subscription.connectors.mocks.MockSubscriptionConnector
import incometax.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import incometax.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import incometax.subscription.models.{SubscriptionFailureResponse, SubscriptionSuccess}
import incometax.subscription.services.SubscriptionService
import models.SummaryModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import core.utils.MockTrait
import core.utils.TestConstants._

import scala.concurrent.Future

trait MockSubscriptionService extends MockTrait {
  val mockSubscriptionService = mock[SubscriptionService]

  private def mockCreateSubscription(nino: String, summaryModel: SummaryModel)(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionService.submitSubscription(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(summaryModel))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockCreateSubscriptionSuccess(nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(nino, summaryModel)(Future.successful(testSubscriptionSuccess))

  def mockCreateSubscriptionFailure(nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(nino, summaryModel)(Future.successful(testSubscriptionFailure))

  def mockCreateSubscriptionException(nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(nino, summaryModel)(Future.failed(testException))

  private def mockGetSubscription(nino: String)(result: Future[GetSubscriptionResponse]): Unit =
    when(mockSubscriptionService.getSubscription(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockGetSubscriptionFound(nino: String): Unit =
    mockGetSubscription(nino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDID)))))

  def setupMockGetSubscriptionNotFound(nino: String): Unit =
    mockGetSubscription(nino)(Future.successful(Right(None)))

  def setupMockGetSubscriptionFailure(nino: String): Unit =
    mockGetSubscription(nino)(Future.successful(Left(SubscriptionFailureResponse(BAD_REQUEST))))

  def setupMockGetSubscriptionException(nino: String): Unit =
    mockGetSubscription(nino)(Future.failed(testException))
}

trait TestSubscriptionService extends MockSubscriptionConnector {

  object TestSubscriptionService extends SubscriptionService(app.injector.instanceOf[Logging], mockSubscriptionConnector)

}
