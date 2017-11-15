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
import core.utils.MockTrait
import core.utils.TestConstants.{testException, testLockoutResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import usermatching.connectors.mocks.MockUserLockoutConnector
import usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import usermatching.models.{LockoutStatusFailureResponse, NotLockedOut}
import usermatching.services.UserLockoutService

import scala.concurrent.Future

trait MockUserLockoutService extends MockTrait {
  val mockUserLockoutService = mock[UserLockoutService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUserLockoutService)
  }

  private def mockLockoutUser(token: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockUserLockoutService.lockoutUser(ArgumentMatchers.eq(token))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockLockCreated(token: String): Unit =
    mockLockoutUser(token)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockFailureResponse(token: String): Unit =
    mockLockoutUser(token)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockException(token: String): Unit =
    mockLockoutUser(token)(Future.failed(testException))

  def verifyLockoutUser(token: String, count: Int): Unit =
    verify(mockUserLockoutService, times(count)).lockoutUser(ArgumentMatchers.eq(token))(ArgumentMatchers.any[HeaderCarrier])

  private def mockGetLockoutStatus(token: String)(result: Future[LockoutStatusResponse]): Unit = {
    when(mockUserLockoutService.getLockoutStatus(ArgumentMatchers.eq(token))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)
  }

  def setupMockNotLockedOut(token: String): Unit =
    mockGetLockoutStatus(token)(Future.successful(Right(NotLockedOut)))

  def setupMockLockedOut(token: String): Unit =
    mockGetLockoutStatus(token)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockStatusFailureResponse(token: String): Unit =
    mockGetLockoutStatus(token)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockStatusException(token: String): Unit =
    mockGetLockoutStatus(token)(Future.failed(testException))

}


trait TestUserLockoutService extends MockUserLockoutConnector {

  object TestUserLockoutService extends UserLockoutService(mockUserLockoutConnector, app.injector.instanceOf[Logging])

}
