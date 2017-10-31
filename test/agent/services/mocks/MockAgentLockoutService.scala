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
import agent.connectors.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import agent.connectors.mocks.{MockAgentLockoutConnector, TestAgentLockoutConnector}
import agent.connectors.models.matching.{LockoutStatusFailureResponse, NotLockedOut}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import agent.services.AgentLockoutService
import core.utils.MockTrait
import agent.utils.TestConstants.{testException, testLockoutResponse}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait MockAgentLockoutService extends MockTrait {
  val mockAgentLockoutService = mock[AgentLockoutService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentLockoutService)
  }

  private def mockLockoutAgent(arn: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockAgentLockoutService.lockoutAgent(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockLockCreated(arn: String): Unit =
    mockLockoutAgent(arn)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockFailureResponse(arn: String): Unit =
    mockLockoutAgent(arn)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockException(arn: String): Unit =
    mockLockoutAgent(arn)(Future.failed(testException))

  def verifyLockoutAgent(arn: String, count: Int): Unit =
    verify(mockAgentLockoutService, times(count)).lockoutAgent(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier])

  private def mockGetLockoutStatus(arn: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockAgentLockoutService.getLockoutStatus(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockNotLockedOut(arn: String): Unit =
    mockGetLockoutStatus(arn)(Future.successful(Right(NotLockedOut)))

  def setupMockLockedOut(arn: String): Unit =
    mockGetLockoutStatus(arn)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockStatusFailureResponse(arn: String): Unit =
    mockGetLockoutStatus(arn)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockStatusException(arn: String): Unit =
    mockGetLockoutStatus(arn)(Future.failed(testException))

}


trait TestAgentLockoutService extends MockAgentLockoutConnector {

  object TestAgentLockoutService extends AgentLockoutService(mockAgentLockoutConnector, app.injector.instanceOf[Logging])

}
