/*
 * Copyright 2018 HM Revenue & Customs
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
import incometax.subscription.connectors.mocks.TestGGAdminConnector
import incometax.subscription.models.{KnownFactsFailure, KnownFactsSuccess}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class GGAdminConnectorSpec extends TestGGAdminConnector with ScalaFutures {
  "GGAdminConnector.addKnownFacts" must {

    "Post to the correct url" in {
      TestGGAdminConnector.addKnownFactsUrl must endWith("/government-gateway-admin/service/HMRC-MTD-IT/known-facts")
    }

    def result: Future[Either[KnownFactsFailure, KnownFactsSuccess.type]] = TestGGAdminConnector.addKnownFacts(knownFactsRequest)

    "parse and return a success response correctly" in {
      mockAddKnownFactsSuccess(knownFactsRequest)
      whenReady(result)(_ mustBe Right(KnownFactsSuccess))
    }

    "parse and return a failure correctly" in {
      mockAddKnownFactsFailure(knownFactsRequest)
      whenReady(result)(_ mustBe Left(KnownFactsFailure(errorJson.toString)))
    }

    "pass through the exception when the call to known facts fails" in {
      mockAddKnownFactsException(knownFactsRequest)
      whenReady(result.failed)(_ mustBe testException)
    }
  }

}
