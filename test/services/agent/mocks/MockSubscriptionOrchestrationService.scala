/*
 * Copyright 2021 HM Revenue & Customs
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

package services.agent.mocks

import models.common.subscription.{SubscriptionFailure, SubscriptionSuccess}
import models.AgentSummary
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.agent.SubscriptionOrchestrationService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.agent.TestConstants._

import scala.concurrent.Future

trait MockSubscriptionOrchestrationService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockSubscriptionOrchestrationService: SubscriptionOrchestrationService = mock[SubscriptionOrchestrationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionOrchestrationService)
  }

  private def mockCreateSubscription(arn: String,
                                     nino: String,
                                     utr: String,
                                     agentSummary: AgentSummary,
                                     isReleaseFourEnabled: Boolean = false
                                    )(result: Future[Either[SubscriptionFailure, SubscriptionSuccess]]): Unit = {
    when(mockSubscriptionOrchestrationService.createSubscription(
      ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(agentSummary),
      ArgumentMatchers.eq(isReleaseFourEnabled),
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn result
  }

  def mockCreateSubscriptionSuccess(arn: String, nino: String, utr: String,
                                    agentSummary: AgentSummary,
                                    isReleaseFourEnabled: Boolean = false
                                   ): Unit = {
    mockCreateSubscription(arn, nino, utr, agentSummary, isReleaseFourEnabled)(Future.successful(testSubscriptionSuccess))
  }

  def mockCreateSubscriptionFailure(arn: String, nino: String, utr: String,
                                    agentSummary: AgentSummary,
                                    isReleaseFourEnabled: Boolean = false
                                   ): Unit = {
    mockCreateSubscription(arn, nino, utr, agentSummary, isReleaseFourEnabled)(Future.successful(testSubscriptionFailure))
  }

  def mockCreateSubscriptionException(arn: String, nino: String, utr: String,
                                      agentSummary: AgentSummary,
                                      isReleaseFourEnabled: Boolean = false
                                     ): Unit = {
    mockCreateSubscription(arn, nino, utr, agentSummary, isReleaseFourEnabled)(Future.failed(testException))
  }
}
