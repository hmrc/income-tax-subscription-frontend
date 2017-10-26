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

package agent.services.mocks

import agent.audit.Logging
import agent.connectors.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import agent.connectors.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import agent.connectors.mocks.MockSubscriptionConnector
import agent.connectors.models.subscription.{SubscriptionFailureResponse, SubscriptionSuccess}
import agent.models.SummaryModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import agent.services.SubscriptionService
import agent.utils.MockTrait
import agent.utils.TestConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait MockSubscriptionService extends MockTrait {
  val mockSubscriptionService = mock[SubscriptionService]

  private def mockCreateSubscription(arn: String, nino: String, summaryModel: SummaryModel)(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionService.submitSubscription(ArgumentMatchers.eq(arn), ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(summaryModel))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockCreateSubscriptionSuccess(arn: String, nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(arn, nino, summaryModel)(Future.successful(testSubscriptionSuccess))

  def mockCreateSubscriptionFailure(arn: String, nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(arn, nino, summaryModel)(Future.successful(testSubscriptionFailure))

  def mockCreateSubscriptionException(arn: String, nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(arn, nino, summaryModel)(Future.failed(testException))

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
