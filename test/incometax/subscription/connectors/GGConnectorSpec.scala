/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.subscription.connectors

import core.utils.TestConstants._
import incometax.subscription.connectors.mocks.TestGGConnector
import incometax.subscription.models.{EnrolFailure, EnrolSuccess}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class GGConnectorSpec extends TestGGConnector with ScalaFutures {
  "GGConnector.enrol" must {
    def result: Future[Either[EnrolFailure, EnrolSuccess.type]] = TestGovernmentGatewayEnrolConnector.enrol(testEnrolRequest)

    "handle when enrol returns a success" in {
      mockEnrolSuccess(testEnrolRequest)
      whenReady(result)(_ mustBe Right(EnrolSuccess))
    }

    "handle when enrol returns an error" in {
      mockEnrolFailure(testEnrolRequest)
      whenReady(result)(_ mustBe Left(EnrolFailure(errorJson.toString())))
    }

    "pass through the exception when the call to enrol fails" in {
      mockEnrolException(testEnrolRequest)
      whenReady(result.failed)(_ mustBe testException)
    }
  }

}
