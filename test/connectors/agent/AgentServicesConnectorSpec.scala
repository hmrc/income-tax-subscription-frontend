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

package connectors.agent

import connectors.agent.mocks.TestAgentServicesConnector
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException
import utilities.agent.TestConstants._

class AgentServicesConnectorSpec extends TestAgentServicesConnector {
  "isPreExistingRelationship" should {
    "return true if the agent and client have a pre existing relationship" in {
      mockIsPreExistingRelationship(testARN, testNino)(OK)

      val res = TestAgentServicesConnector.isPreExistingRelationship(testARN, testNino)

      await(res) mustBe true
    }

    "return false if the agent and client do not have a pre existing relationship" in {
      mockIsPreExistingRelationship(testARN, testNino)(NOT_FOUND)

      val res = TestAgentServicesConnector.isPreExistingRelationship(testARN, testNino)

      await(res) mustBe false
    }

    "return a failure on a non OK status" in {
      val invalidBody = Json.toJson("invalid")
      mockIsPreExistingRelationship(testARN, testNino)(INTERNAL_SERVER_ERROR, invalidBody)

      val res = TestAgentServicesConnector.isPreExistingRelationship(testARN, testNino)

      val ex = intercept[InternalServerException](await(res))
      ex.getMessage mustBe s"[AgentServicesConnector][isPreExistingRelationship] failure, status: $INTERNAL_SERVER_ERROR body=$invalidBody"
    }
  }
}
