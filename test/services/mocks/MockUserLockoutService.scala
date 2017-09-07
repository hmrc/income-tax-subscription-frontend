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

package services.mocks

import audit.Logging
import connectors.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import connectors.mocks.TestUserLockoutConnector
import connectors.models.matching.{LockoutStatusFailureResponse, NotLockedOut}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import services.UserLockoutService
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.MockTrait
import utils.TestConstants.{testException, testLockoutResponse}

import scala.concurrent.Future

trait MockUserLockoutService extends MockTrait {
  val mockUserLockoutService = mock[UserLockoutService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUserLockoutService)
  }

  private def mockLockoutAgent(bearerToken: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockUserLockoutService.lockoutUser(ArgumentMatchers.eq(bearerToken))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockLockCreated(bearerToken: String): Unit =
    mockLockoutAgent(bearerToken)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockFailureResponse(bearerToken: String): Unit =
    mockLockoutAgent(bearerToken)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockException(bearerToken: String): Unit =
    mockLockoutAgent(bearerToken)(Future.failed(testException))

  def verifyLockoutUser(bearerToken: String, count: Int): Unit =
    verify(mockUserLockoutService, times(count)).lockoutUser(ArgumentMatchers.eq(bearerToken))(ArgumentMatchers.any[HeaderCarrier])

  private def mockGetLockoutStatus(bearerToken: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockUserLockoutService.getLockoutStatus(ArgumentMatchers.eq(bearerToken))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockNotLockedOut(bearerToken: String): Unit =
    mockGetLockoutStatus(bearerToken)(Future.successful(Right(NotLockedOut)))

  def setupMockLockedOut(bearerToken: String): Unit =
    mockGetLockoutStatus(bearerToken)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockStatusFailureResponse(bearerToken: String): Unit =
    mockGetLockoutStatus(bearerToken)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockStatusException(bearerToken: String): Unit =
    mockGetLockoutStatus(bearerToken)(Future.failed(testException))

}


trait TestUserLockoutService extends TestUserLockoutConnector {

  object TestUserLockoutService extends UserLockoutService(mockAgentLockoutConnector, app.injector.instanceOf[Logging])

}
