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

package agent.services

import agent.connectors.models.gg._
import org.scalatest.concurrent.ScalaFutures
import agent.services.mocks.TestKnownFactsService
import agent.utils.TestConstants._
import core.utils.UnitTestTrait

import scala.concurrent.Future

class KnownFactsServiceSpec extends UnitTestTrait with TestKnownFactsService with ScalaFutures {
  "addKnownFacts" must {
    def result: Future[Either[KnownFactsFailure, KnownFactsSuccess.type]] = TestKnownFactsService.addKnownFacts(testMTDID, testNino)

    "return a success from the GGAdminConnector" in {
      mockAddKnownFactsSuccess(expectedRequestModel)

      whenReady(result)(_ mustBe Right(KnownFactsSuccess))
    }

    "return a failure from the GGAdminConnector" in {
      mockAddKnownFactsFailure(expectedRequestModel)

      whenReady(result)(_ mustBe Left(KnownFactsFailure(testErrorMessage)))
    }

    "pass through the exception if the GGAdminConnector fails" in {
      mockAddKnownFactsException(expectedRequestModel)

      whenReady(result.failed)(_ mustBe testException)
    }
  }
}
