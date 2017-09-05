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

import connectors.models.matching.UserMatchSuccessResponseModel
import play.api.test.Helpers._
import services.mocks.TestUserMatchingService
import uk.gov.hmrc.play.http.InternalServerException
import utils.TestConstants
import utils.TestModels._

class UserMatchingServiceSpec extends TestUserMatchingService {

  "UserMatchingService" should {

    "return the nino if authenticator response with ok" in {
      setupMatchClient(matchClientMatched(TestConstants.testNino))
      val result = TestUserMatchingService.matchClient(testUserDetails)
      await(result) mustBe Right(Some(testMatchSuccessModel))
    }

    "return None if authenticator response with Unauthorized but with a matching error message" in {
      setupMatchClient(matchClientNoMatch)
      val result = TestUserMatchingService.matchClient(testUserDetails)
      await(result) mustBe Right(None)
    }

    "throw InternalServerException if authenticator response with Unauthorized but with a server error message" in {
      setupMatchClient(matchClientUnexpectedFailure)
      val result = TestUserMatchingService.matchClient(testUserDetails)

      val e = intercept[InternalServerException] {
        await(result)
      }
      e.message must include (s"AuthenticatorConnector.matchClient unexpected response from authenticator: status=$UNAUTHORIZED")
    }

    "throw InternalServerException if authenticator response with an unexpected status" in {
      setupMatchClient(matchClientUnexpectedFailure)
      val result = TestUserMatchingService.matchClient(testUserDetails)

      val e = intercept[InternalServerException] {
        await(result)
      }
      e.message must include (s"AuthenticatorConnector.matchClient unexpected response from authenticator: status=")
      e.message must not include s"UNAUTHORIZED"
    }
  }

}
