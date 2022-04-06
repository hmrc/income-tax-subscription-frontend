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

package services

import connectors.usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import models.usermatching.{LockoutStatusFailure, LockoutStatusFailureResponse, NotLockedOut}
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers._
import play.api.test.Helpers._
import services.mocks.TestUserLockoutService
import utilities.individual.TestConstants._

class UserLockOutServiceSpec extends TestUserLockoutService with EitherValues {

  "UserLockoutService.getLockOutStatus" should {

    def call: LockoutStatusResponse = await(TestUserLockoutService.getLockoutStatus(token = testUserId))

    "return the not locked out status" in {
      setupMockNotLockedOut(escapedUserId)
      call.value shouldBe NotLockedOut
    }

    "return the locked out status" in {
      setupMockLockedOut(escapedUserId)
      call.value shouldBe testLockoutResponse
    }

    "return the error if lock status fails on bad request" in {
      setupMockLockStatusFailureResponse(escapedUserId)
      call.left.value shouldBe LockoutStatusFailureResponse(BAD_REQUEST)
    }

    "return the error if locked out throws an exception" in {
      setupMockLockStatusException(escapedUserId)
      intercept[Exception](call) shouldBe testException
    }
  }

  "UserLockoutService.incrementLockout" should {

    def call(counter: Int): Either[LockoutStatusFailure, LockoutUpdate] = await(TestUserLockoutService.incrementLockout(token = testUserId, counter))

    "when counter is under the limit should return not locked out and updated new counter, should not clear Subscription Details " in {
      setupMockLockCreated(escapedUserId)
      call(appConfig.matchingAttempts - 2).value shouldBe LockoutUpdate(NotLockedOut, Some(appConfig.matchingAttempts - 1))
    }

    "when counter exceeds max should return locked out, Subscription Details  should be cleared" in {
      setupMockLockCreated(escapedUserId)
      call(appConfig.matchingAttempts - 1).value shouldBe LockoutUpdate(testLockoutResponse, None)
    }

    "return the error if create lock fails on bad request, should not clear Subscription Details " in {
      setupMockLockFailureResponse(escapedUserId)
      call(appConfig.matchingAttempts - 1).left.value shouldBe LockoutStatusFailureResponse(BAD_REQUEST)
    }

    "return the error if create lock throws an exception, should not clear Subscription Details " in {
      setupMockLockException(escapedUserId)
      intercept[Exception](call(appConfig.matchingAttempts - 1)) shouldBe testException
    }
  }

}
