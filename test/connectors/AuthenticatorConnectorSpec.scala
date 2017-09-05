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

package connectors

import connectors.mocks.TestAuthenticatorConnector
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.InternalServerException
import utils.TestModels._

class AuthenticatorConnectorSpec extends TestAuthenticatorConnector {

  "AuthenticatorConnector" should {

    "return true if authenticator response with ok" in {
      setupMockMatchClient(testUserDetails)(matchClientMatched(testUserDetails.nino))
      val result = TestAuthenticatorConnector.matchClient(testUserDetails)
      await(result) mustBe Some(testUserDetails.nino)
    }

    "return false if authenticator response with Unauthorized but with a matching error message" in {
      setupMockMatchClient(testUserDetails)(matchClientNoMatch)
      val result = TestAuthenticatorConnector.matchClient(testUserDetails)
      await(result) mustBe None
    }

    "throw InternalServerException if authenticator response with Unauthorized but with a server error message" in {
      setupMockMatchClient(testUserDetails)(matchClientUnexpectedFailure)
      val result = TestAuthenticatorConnector.matchClient(testUserDetails)

      val e = intercept[InternalServerException] {
        await(result)
      }
      e.message must include (s"AuthenticatorConnector.matchClient unexpected response from authenticator: status=$UNAUTHORIZED")
    }

    "throw InternalServerException if authenticator response with an unexpected status" in {
      setupMockMatchClient(testUserDetails)(matchClientUnexpectedFailure)
      val result = TestAuthenticatorConnector.matchClient(testUserDetails)

      val e = intercept[InternalServerException] {
        await(result)
      }
      e.message must include (s"AuthenticatorConnector.matchClient unexpected response from authenticator: status=")
      e.message must not include s"UNAUTHORIZED"
    }

  }

}
