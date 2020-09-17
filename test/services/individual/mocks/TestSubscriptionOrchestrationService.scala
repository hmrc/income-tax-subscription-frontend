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
import models.{ConnectorError, SummaryModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import services.individual.SubscriptionOrchestrationService
import services.mocks.MockSubscriptionService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

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

trait MockSubscriptionOrchestrationService extends UnitTestTrait with MockitoSugar {
  val mockSubscriptionOrchestrationService = mock[SubscriptionOrchestrationService]

  private def mockCreateSubscription(nino: String,
                                     summaryModel: SummaryModel,
                                     isReleaseFourEnabled: Boolean = false
                                    )(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionOrchestrationService
      .createSubscription(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(summaryModel), ArgumentMatchers.eq(isReleaseFourEnabled)
      )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockCreateSubscriptionSuccess(nino: String, summaryModel: SummaryModel, isReleaseFourEnabled: Boolean): Unit =
    mockCreateSubscription(nino, summaryModel, isReleaseFourEnabled)(Future.successful(testSubscriptionSuccess))

  def mockCreateSubscriptionFailure(nino: String, summaryModel: SummaryModel, isReleaseFourEnabled: Boolean): Unit =
    mockCreateSubscription(nino, summaryModel, isReleaseFourEnabled)(Future.successful(testSubscriptionFailure))

  def mockCreateSubscriptionException(nino: String, summaryModel: SummaryModel, isReleaseFourEnabled: Boolean): Unit =
    mockCreateSubscription(nino, summaryModel, isReleaseFourEnabled)(Future.failed(testException))

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
