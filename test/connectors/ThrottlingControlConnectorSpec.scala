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

import auth.authenticatedFakeRequest
import connectors.mocks.MockThrottlingControlConnector
import connectors.models.throttling.{CanAccess, LimitReached}
import org.scalatest.Matchers._
import play.api.test.Helpers._
import utils.{TestConstants, UnitTestTrait}

class ThrottlingControlConnectorSpec extends UnitTestTrait
  with MockThrottlingControlConnector {

  "ThrottlingControlConnector" should {

    def call = await(TestThrottlingControlConnector.checkAccess(TestConstants.testNino))

    implicit lazy val request = authenticatedFakeRequest()

    "return CanAccess if the service returns an OK" in {
      val enrolment = CanAccess
      setupMockCheckAccess(TestConstants.testNino)(OK)
      call shouldBe Some(enrolment)
    }

    "return LimitReached if the service returns TOO_MANY_REQUESTS" in {
      setupMockCheckAccess(TestConstants.testNino)(TOO_MANY_REQUESTS)
      call shouldBe Some(LimitReached)
    }

    "return None for a different status unsuccessful" in {
      setupMockCheckAccess(TestConstants.testNino)(BAD_REQUEST)
      call shouldBe None
    }
  }

}
