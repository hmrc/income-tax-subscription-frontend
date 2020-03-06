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

package services.individual.mocks

import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import core.connectors.models.ConnectorError
import core.utils.MockTrait
import core.utils.TestConstants._
import models.individual.subscription.SummaryModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.individual.SubscriptionOrchestrationService
import services.mocks.MockSubscriptionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait TestSubscriptionOrchestrationService extends MockSubscriptionService
  with MockKnownFactsService
  with MockEnrolmentService {

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockKnownFactsService,
    mockEnrolmentService
  )

}

trait MockSubscriptionOrchestrationService extends MockTrait {
  val mockSubscriptionOrchestrationService = mock[SubscriptionOrchestrationService]

  private def mockCreateSubscription(nino: String,
                                     summaryModel: SummaryModel
                                    )(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionOrchestrationService
      .createSubscription(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(summaryModel)
      )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockCreateSubscriptionSuccess(nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(nino, summaryModel)(Future.successful(testSubscriptionSuccess))

  def mockCreateSubscriptionFailure(nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(nino, summaryModel)(Future.successful(testSubscriptionFailure))

  def mockCreateSubscriptionException(nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(nino, summaryModel)(Future.failed(testException))

  private def mockEnrolAndRefresh(mtditId: String, nino: String)(result: Future[Either[ConnectorError, String]]): Unit =
    when(
      mockSubscriptionOrchestrationService.enrolAndRefresh
      (ArgumentMatchers.eq(mtditId), ArgumentMatchers.eq(nino))
      (ArgumentMatchers.any[HeaderCarrier])
    )
      .thenReturn(result)

  def mockEnrolAndRefreshSuccess(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.successful(Right(mtditId)))

  def mockEnrolFailure(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.successful(testEnrolFailure))

  def mockRefreshFailure(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.successful(testRefreshProfileFailure))

  def mockEnrolAndRefreshException(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.failed(testException))
}
