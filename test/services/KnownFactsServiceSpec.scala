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

import common.Constants.GovernmentGateway._
import connectors.mocks.MockGGAdminConnector
import connectors.models.gg._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import utils.TestConstants._
import utils.UnitTestTrait

import scala.concurrent.Future

class KnownFactsServiceSpec extends PlaySpec with UnitTestTrait with MockGGAdminConnector with ScalaFutures {
  object TestKnownFactsService extends KnownFactsService(mockGGAdminConnector)

  val expectedRequestModel = KnownFactsRequest(List(
    TypeValuePair(MTDITID, testMTDID),
    TypeValuePair(NINO, testNino)
  ))

  "addKnownFacts" must {
    def result: Future[KnownFactsResponse] = TestKnownFactsService.addKnownFacts(testMTDID, testNino)

    "return a success from the GGAdminConnector" in {
      mockAddKnownFactsSuccess(expectedRequestModel)

      whenReady(result)(_ mustBe KnownFactsSuccess)
    }

    "return a failure from the GGAdminConnector" in {
      mockAddKnownFactsFailure(expectedRequestModel)

      whenReady(result)(_ mustBe KnownFactsFailure(testErrorMessage))
    }

    "pass through the exception if the GGAdminConnector fails" in {
      mockAddKnownFactsException(expectedRequestModel)

      whenReady(result.failed)(_ mustBe testException)
    }
  }
}
