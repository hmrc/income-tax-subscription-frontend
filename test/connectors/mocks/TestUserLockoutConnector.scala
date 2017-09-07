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

package connectors.mocks

import config.AppConfig
import connectors.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import connectors.matching.UserLockoutConnector
import connectors.models.matching.{LockoutStatusFailureResponse, NotLockedOut}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.MockTrait
import utils.TestConstants._

import scala.concurrent.Future

trait TestUserLockoutConnector extends MockTrait with MockHttp {
  val mockAgentLockoutConnector = mock[UserLockoutConnector]

  object TestAgentLockoutConnector extends UserLockoutConnector(
    app.injector.instanceOf[AppConfig],
    mockHttpGet,
    mockHttpPost
  )


  private def setupLockoutAgent(arn: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockAgentLockoutConnector.lockoutUser(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockLockCreated(arn: String): Unit =
    setupLockoutAgent(arn)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockFailureResponse(arn: String): Unit =
    setupLockoutAgent(arn)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockException(arn: String): Unit =
    setupLockoutAgent(arn)(Future.failed(testException))

  private def setupMockGetLockoutStatus(arn: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockAgentLockoutConnector.getLockoutStatus(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockNotLockedOut(arn: String): Unit =
    setupMockGetLockoutStatus(arn)(Future.successful(Right(NotLockedOut)))

  def setupMockLockedOut(arn: String): Unit =
    setupMockGetLockoutStatus(arn)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockStatusFailureResponse(arn: String): Unit =
    setupMockGetLockoutStatus(arn)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockStatusException(arn: String): Unit =
    setupMockGetLockoutStatus(arn)(Future.failed(testException))

}
