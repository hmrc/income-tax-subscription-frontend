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

import agent.connectors.models.ConnectorError
import agent.connectors.models.subscription.SubscriptionSuccess
import org.scalatest.concurrent.ScalaFutures
import agent.services.mocks.TestSubscriptionOrchestrationService
import agent.utils.TestConstants._
import agent.utils.TestModels._
import agent.utils.UnitTestTrait

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends UnitTestTrait with ScalaFutures
  with TestSubscriptionOrchestrationService {

  "createSubscription" should {
    def res: Future[Either[ConnectorError, SubscriptionSuccess]] =
      TestSubscriptionOrchestrationService.createSubscription(testARN, testNino, testSummaryData)

    "return a success when all services succeed" in {
      mockCreateSubscriptionSuccess(testARN, testNino, testSummaryData)
      mockAddKnownFactsSuccess(testMTDID, testNino)

      whenReady(res)(_ mustBe testSubscriptionSuccess)
    }

    "return a failure" when {
      "create subscription returns an error" in {
        mockCreateSubscriptionFailure(testARN, testNino, testSummaryData)

        whenReady(res)(_ mustBe testSubscriptionFailure)
      }

      "create subscription returns an exception" in {
        mockCreateSubscriptionException(testARN, testNino, testSummaryData)

        whenReady(res.failed)(_ mustBe testException)
      }

      "add known facts returns an error" in {
        mockCreateSubscriptionSuccess(testARN, testNino, testSummaryData)
        mockAddKnownFactsFailure(testMTDID, testNino)

        whenReady(res)(_ mustBe testKnownFactsFailure)
      }

      "add known facts returns an exception" in {
        mockCreateSubscriptionSuccess(testARN, testNino, testSummaryData)
        mockAddKnownFactsException(testMTDID, testNino)

        whenReady(res.failed)(_ mustBe testException)
      }
    }
  }

}
