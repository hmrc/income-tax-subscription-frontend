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

package services

import connectors.models.matching.{UserMatchFailureResponseModel, UserMatchUnexpectedError}
import play.api.test.Helpers._
import services.mocks.TestUserMatchingService
import utils.TestModels._

class UserMatchingServiceSpec extends TestUserMatchingService {

  "ClientMatchingService" should {

    "return the user with nino and utr if authenticator response with ok with both ids" in {
      mockUserMatchSuccess(testUserDetails)
      val result = TestUserMatchingService.matchClient(testUserDetails)
      await(result) mustBe Right(Some(testMatchSuccessModel))
    }

    "return the nino if authenticator response with ok with only nino" in {
      mockUserMatchNoUtr(testUserDetails)
      val result = TestUserMatchingService.matchClient(testUserDetails)
      await(result) mustBe Right(Some(testMatchNoUtrModel))
    }

    "return None if authenticator response with Unauthorized but with a matching error message" in {
      mockUserMatchNotFound(testUserDetails)
      val result = TestUserMatchingService.matchClient(testUserDetails)
      await(result) mustBe Right(None)
    }

    "return Left(error) if authenticator response with an error status" in {
      mockUserMatchFailure(testUserDetails)
      val result = TestUserMatchingService.matchClient(testUserDetails)
      await(result) mustBe Left(UserMatchUnexpectedError)
    }

    "throw InternalServerException if authenticator response with an unexpected status" in {
      mockUserMatchException(testUserDetails)
      val result = TestUserMatchingService.matchClient(testUserDetails)

      val e = intercept[Exception] {
        await(result)
      }
    }
  }

}
