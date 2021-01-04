/*
 * Copyright 2021 HM Revenue & Customs
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

package services.agent

import play.api.test.Helpers._
import services.agent.mocks.TestClientRelationshipService
import utilities.agent.TestConstants._

class ClientRelationshipServiceSpec extends TestClientRelationshipService {
  "isPreExistingRelationship" should {
    "return true if the connector returns true" in {
      preExistingRelationship(testARN, testNino)(isPreExistingRelationship = true)

      val res = TestClientRelationshipService.isPreExistingRelationship(testARN, testNino)

      await(res) mustBe true
    }

    "return false if the connector returns false" in {
      preExistingRelationship(testARN, testNino)(isPreExistingRelationship = false)

      val res = TestClientRelationshipService.isPreExistingRelationship(testARN, testNino)

      await(res) mustBe false
    }

    "return a failed future if the connection fails" in {
      val exception = new Exception()

      preExistingRelationshipFailure(testARN, testNino)(exception)

      val res = TestClientRelationshipService.isPreExistingRelationship(testARN, testNino)

      intercept[Exception](await(res)) mustBe exception
    }
  }

}
