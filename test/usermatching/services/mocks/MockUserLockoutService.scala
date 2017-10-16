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

package usermatching.services.mocks

import core.audit.Logging
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, UserId}
import usermatching.connectors.mocks.MockUserLockoutConnector
import usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import usermatching.models.{LockoutStatusFailureResponse, NotLockedOut}
import usermatching.services.UserLockoutService
import utils.MockTrait
import utils.TestConstants.{testException, testLockoutResponse}

import scala.concurrent.Future

trait MockUserLockoutService extends MockTrait {
  val mockUserLockoutService = mock[UserLockoutService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUserLockoutService)
  }

  private def mockLockoutUser(userId: UserId)(result: Future[LockoutStatusResponse]): Unit =
    when(mockUserLockoutService.lockoutUser(UserId(ArgumentMatchers.eq(userId.value)))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockLockCreated(userId: UserId): Unit =
    mockLockoutUser(userId)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockFailureResponse(userId: UserId): Unit =
    mockLockoutUser(userId)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockException(userId: UserId): Unit =
    mockLockoutUser(userId)(Future.failed(testException))

  def verifyLockoutUser(userId: UserId, count: Int): Unit =
    verify(mockUserLockoutService, times(count)).lockoutUser(UserId(ArgumentMatchers.eq(userId.value)))(ArgumentMatchers.any[HeaderCarrier])

  private def mockGetLockoutStatus(userId: UserId)(result: Future[LockoutStatusResponse]): Unit = {
    when(mockUserLockoutService.getLockoutStatus(UserId(ArgumentMatchers.eq(userId.value)))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)
  }

  def setupMockNotLockedOut(userId: UserId): Unit =
    mockGetLockoutStatus(userId)(Future.successful(Right(NotLockedOut)))

  def setupMockLockedOut(userId: UserId): Unit =
    mockGetLockoutStatus(userId)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockStatusFailureResponse(userId: UserId): Unit =
    mockGetLockoutStatus(userId)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockStatusException(userId: UserId): Unit =
    mockGetLockoutStatus(userId)(Future.failed(testException))

}


trait TestUserLockoutService extends MockUserLockoutConnector {

  object TestUserLockoutService extends UserLockoutService(mockUserLockoutConnector, app.injector.instanceOf[Logging])

}
