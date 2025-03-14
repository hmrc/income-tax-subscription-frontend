/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import connectors.usermatching.mocks.MockUserLockoutConnector
import models.usermatching.{LockoutStatusFailure, LockoutStatusFailureResponse, NotLockedOut}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import services.{LockoutUpdate, UserLockoutService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UserMatchingTestSupport
import utilities.individual.TestConstants.{testException, testLockoutResponse}

import scala.concurrent.{ExecutionContext, Future}

trait MockUserLockoutService extends MockitoSugar with BeforeAndAfterEach with UserMatchingTestSupport {
  suite: Suite with Matchers =>

  val mockUserLockoutService: UserLockoutService = mock[UserLockoutService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUserLockoutService)
  }

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

  private def mockIncrementLockout(token: String, currentFailedMatches: Int)(result: Future[Either[LockoutStatusFailure, LockoutUpdate]]): Unit = {
    when(mockUserLockoutService.incrementLockout(
      ArgumentMatchers.eq(token),
      ArgumentMatchers.eq(currentFailedMatches)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[ExecutionContext])
    ).thenReturn(result)
  }

  def setupIncrementNotLockedOut(token: String, currentFailedMatches: Int, maxAttempts: Int): Unit = {
    val returnedFailures: Option[Int] = if (maxAttempts - 1 == currentFailedMatches) None else Some(currentFailedMatches + 1)
    mockIncrementLockout(token, currentFailedMatches)(Future.successful(Right(LockoutUpdate(NotLockedOut, returnedFailures))))
  }

  def setupIncrementLockedOut(token: String, currentFailedMatches: Int, maxAttempts: Int): Unit = {
    val returnedFailures: Option[Int] = if (maxAttempts - 1 == currentFailedMatches) None else Some(currentFailedMatches + 1)
    mockIncrementLockout(token, currentFailedMatches)(Future.successful(Right(LockoutUpdate(testLockoutResponse, returnedFailures))))
  }

  def setupIncrementLockedOutFailure(token: String, currentFailedMatches: Int): Unit = {
    mockIncrementLockout(token, currentFailedMatches)(Future.successful(Left(LockoutStatusFailureResponse(500))))
  }

  def verifyIncrementLockout(token: String, count: Int): Unit =
    verify(mockUserLockoutService, times(count)).incrementLockout(ArgumentMatchers.eq(token),
      ArgumentMatchers.any[Int])(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[ExecutionContext])

}


trait TestUserLockoutService extends MockUserLockoutConnector
  with MockSubscriptionDetailsService {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  object TestUserLockoutService extends UserLockoutService(
    appConfig,
    mockUserLockoutConnector,
    mockSubscriptionDetailsService
  )

}
