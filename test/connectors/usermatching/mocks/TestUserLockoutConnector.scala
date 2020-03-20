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

package connectors.usermatching.mocks

import auth.MockHttp
import config.AppConfig
import connectors.usermatching.UserLockoutConnector
import connectors.usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import utilities.individual.TestConstants._
import models.usermatching.{LockoutStatusFailureResponse, NotLockedOut}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.HeaderCarrier
import utilities.MockTrait

import scala.concurrent.Future

trait MockUserLockoutConnector extends MockTrait {
  val mockUserLockoutConnector: UserLockoutConnector = mock[UserLockoutConnector]

  private def setupLockoutUser(arn: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockUserLockoutConnector.lockoutUser(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockLockCreated(token: String): Unit =
    setupLockoutUser(token)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockFailureResponse(token: String): Unit =
    setupLockoutUser(token)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockException(token: String): Unit =
    setupLockoutUser(token)(Future.failed(testException))

  private def setupMockGetLockoutStatus(token: String)(result: Future[LockoutStatusResponse]): Unit =
    when(mockUserLockoutConnector.getLockoutStatus(ArgumentMatchers.eq(token))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockNotLockedOut(token: String): Unit =
    setupMockGetLockoutStatus(token)(Future.successful(Right(NotLockedOut)))

  def setupMockLockedOut(token: String): Unit =
    setupMockGetLockoutStatus(token)(Future.successful(Right(testLockoutResponse)))

  def setupMockLockStatusFailureResponse(token: String): Unit =
    setupMockGetLockoutStatus(token)(Future.successful(Left(LockoutStatusFailureResponse(BAD_REQUEST))))

  def setupMockLockStatusException(token: String): Unit =
    setupMockGetLockoutStatus(token)(Future.failed(testException))

}

trait TestUserLockoutConnector extends MockTrait with MockHttp {

  object TestUserLockoutConnector extends UserLockoutConnector(
    app.injector.instanceOf[AppConfig],
    mockHttp
  )

}
