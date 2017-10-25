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

package agent.connectors

import agent.connectors.mocks.TestAuthenticatorConnector
import play.api.test.Helpers._
import agent.utils.TestModels.testClientDetails
import uk.gov.hmrc.http.InternalServerException


class AuthenticatorConnectorSpec extends TestAuthenticatorConnector {

  "AuthenticatorConnector" should {

    "return true if authenticator response with ok" in {
      setupMockMatchClient(testClientDetails)(matchClientMatched(testClientDetails.nino))
      val result = TestAuthenticatorConnector.matchClient(testClientDetails)
      await(result) mustBe Some(testClientDetails.nino)
    }

    "return false if authenticator response with Unauthorized but with a matching error message" in {
      setupMockMatchClient(testClientDetails)(matchClientNoMatch)
      val result = TestAuthenticatorConnector.matchClient(testClientDetails)
      await(result) mustBe None
    }

    "throw InternalServerException if authenticator response with Unauthorized but with a server error message" in {
      setupMockMatchClient(testClientDetails)(matchClientUnexpectedFailure)
      val result = TestAuthenticatorConnector.matchClient(testClientDetails)

      val e = intercept[InternalServerException] {
        await(result)
      }
      e.message must include (s"AuthenticatorConnector.matchClient unexpected response from authenticator: status=$UNAUTHORIZED")
    }

    "throw InternalServerException if authenticator response with an unexpected status" in {
      setupMockMatchClient(testClientDetails)(matchClientUnexpectedFailure)
      val result = TestAuthenticatorConnector.matchClient(testClientDetails)

      val e = intercept[InternalServerException] {
        await(result)
      }
      e.message must include (s"AuthenticatorConnector.matchClient unexpected response from authenticator: status=")
      e.message must not include s"UNAUTHORIZED"
    }

  }

}
