/*
 * Copyright 2019 HM Revenue & Customs
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

import agent.services.SubscriptionOrchestrationService
import agent.utils.TestConstants._
import core.utils.MockTrait
import incometax.subscription.models.{SubscriptionFailureResponse, SubscriptionSuccess, SummaryModel}
import incometax.subscription.services.mocks.{MockKnownFactsService, MockSubscriptionService}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait TestSubscriptionOrchestrationService extends MockSubscriptionService
  with MockKnownFactsService {

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockKnownFactsService
  )

}

trait MockSubscriptionOrchestrationService extends MockTrait {

  val mockSubscriptionOrchestrationService: SubscriptionOrchestrationService = mock[SubscriptionOrchestrationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionOrchestrationService)
  }

  private def mockCreateSubscription(arn: String,
                                     nino: String,
                                     summaryModel: SummaryModel
                                    )(result: Future[Either[SubscriptionFailureResponse, SubscriptionSuccess]]): Unit =
    when(mockSubscriptionOrchestrationService
      .createSubscription(ArgumentMatchers.eq(arn), ArgumentMatchers.eq(nino), ArgumentMatchers.eq(summaryModel)
      )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockCreateSubscriptionSuccess(arn: String, nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(arn, nino, summaryModel)(Future.successful(testSubscriptionSuccess))

  def mockCreateSubscriptionFailure(arn: String, nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(arn, nino, summaryModel)(Future.successful(testSubscriptionFailure))

  def mockCreateSubscriptionException(arn: String, nino: String, summaryModel: SummaryModel): Unit =
    mockCreateSubscription(arn, nino, summaryModel)(Future.failed(testException))
}
