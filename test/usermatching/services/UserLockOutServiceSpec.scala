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

package usermatching.services

import connectors.usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import core.utils.TestConstants._
import models.usermatching.{LockoutStatusFailure, LockoutStatusFailureResponse, NotLockedOut}
import org.scalatest.EitherValues
import org.scalatest.Matchers._
import play.api.mvc.Result
import play.api.test.Helpers._
import usermatching.services.mocks.TestUserLockoutService

import scala.concurrent.Future

class UserLockOutServiceSpec extends TestUserLockoutService with EitherValues {

  "UserLockoutService.getLockOutStatus" should {

    def call: LockoutStatusResponse = await(TestUserLockoutService.getLockoutStatus(token = testUserId.value))

    "return the not locked out status" in {
      setupMockNotLockedOut(escapedUserId)
      call.right.value shouldBe NotLockedOut
    }

    "return the locked out status" in {
      setupMockLockedOut(escapedUserId)
      call.right.value shouldBe testLockoutResponse
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

    def call(counter: Int): Either[LockoutStatusFailure, LockoutUpdate] = await(TestUserLockoutService.incrementLockout(token = testUserId.value, counter))

    "when counter is under the limit should return not locked out and updated new counter, should not clear keystore" in {
      setupMockLockCreated(escapedUserId)
      call(appConfig.matchingAttempts - 2).right.value shouldBe LockoutUpdate(NotLockedOut, appConfig.matchingAttempts - 1)

      verifyKeystore(deleteAll = 0)
    }

    "when counter exceeds max should return locked out, keystore should be cleared" in {
      setupMockLockCreated(escapedUserId)
      call(appConfig.matchingAttempts - 1).right.value shouldBe LockoutUpdate(testLockoutResponse, None)

      verifyKeystore(deleteAll = 1)
    }

    "return the error if create lock fails on bad request, should not clear keystore" in {
      setupMockLockFailureResponse(escapedUserId)
      call(appConfig.matchingAttempts - 1).left.value shouldBe LockoutStatusFailureResponse(BAD_REQUEST)

      verifyKeystore(deleteAll = 0)
    }

    "return the error if create lock throws an exception, should not clear keystore" in {
      setupMockLockException(escapedUserId)
      intercept[Exception](call(appConfig.matchingAttempts - 1)) shouldBe testException

      verifyKeystore(deleteAll = 0)

    }
  }

}
