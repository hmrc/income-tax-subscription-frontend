/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.http.Status._
import services.{LockoutUpdate, UserLockoutService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utilities.individual.TestConstants.{testException, testLockoutResponse}
import utilities.{UnitTestTrait, UserMatchingTestSupport}

import scala.concurrent.{ExecutionContext, Future}

trait MockUserLockoutService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach with UserMatchingTestSupport {

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

  def setupIncrementNotLockedOut(token: String, currentFailedMatches: Int): Unit = {
    val returnedFailures: Option[Int] = if(appConfig.matchingAttempts - 1 == currentFailedMatches) None else Some(currentFailedMatches + 1)
    mockIncrementLockout(token, currentFailedMatches)(Future.successful(Right(LockoutUpdate(NotLockedOut, returnedFailures))))
  }

  def setupIncrementLockedOut(token: String, currentFailedMatches: Int): Unit = {
    val returnedFailures: Option[Int] = if(appConfig.matchingAttempts - 1 == currentFailedMatches) None else Some(currentFailedMatches + 1)
    mockIncrementLockout(token, currentFailedMatches)(Future.successful(Right(LockoutUpdate(testLockoutResponse, returnedFailures))))
  }

  def verifyIncrementLockout(token: String, count: Int): Unit =
    verify(mockUserLockoutService, times(count)).incrementLockout(ArgumentMatchers.eq(token),
      ArgumentMatchers.any[Int])(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[ExecutionContext])

}


trait TestUserLockoutService extends MockUserLockoutConnector
  with MockSubscriptionDetailsService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockDeleteAllFromSubscriptionDetails(HttpResponse(Status.OK))
  }

  object TestUserLockoutService extends UserLockoutService(
    appConfig,
    mockUserLockoutConnector,
    MockSubscriptionDetailsService
  )

}
