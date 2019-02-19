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

package incometax.subscription.services

import core.connectors.models.ConnectorError
import incometax.subscription.models.SubscriptionSuccess
import incometax.subscription.services.mocks.TestSubscriptionOrchestrationService
import org.scalatest.concurrent.ScalaFutures
import core.utils.TestConstants._
import core.utils.TestModels._
import core.utils.UnitTestTrait

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends UnitTestTrait with ScalaFutures
  with TestSubscriptionOrchestrationService {

  "createSubscription for an individual" should {
    def res: Future[Either[ConnectorError, SubscriptionSuccess]] =
      TestSubscriptionOrchestrationService.createSubscription(testNino, testSummaryData)

    "return a success when all incometax.business.services succeed" in {
      mockCreateSubscriptionSuccess(testNino, testSummaryData, None)
      mockAddKnownFactsSuccess(testMTDID, testNino)
      mockEnrolSuccess(testMTDID, testNino)

      whenReady(res)(_ mustBe testSubscriptionSuccess)
    }

    "return a failure" when {
      "create subscription returns an error" in {
        mockCreateSubscriptionFailure(testNino, testSummaryData, None)

        whenReady(res)(_ mustBe testSubscriptionFailure)
      }

      "create subscription returns an exception" in {
        mockCreateSubscriptionException(testNino, testSummaryData, None)

        whenReady(res.failed)(_ mustBe testException)
      }

      "add known facts returns an error" in {
        mockCreateSubscriptionSuccess(testNino, testSummaryData, None)
        mockAddKnownFactsFailure(testMTDID, testNino)

        whenReady(res)(_ mustBe testKnownFactsFailure)
      }

      "add known facts returns an exception" in {
        mockCreateSubscriptionSuccess(testNino, testSummaryData, None)
        mockAddKnownFactsException(testMTDID, testNino)

        whenReady(res.failed)(_ mustBe testException)
      }
    }
  }

  "createSubscription for individual confirming an unauthorised agent's submission" should {

    def res: Future[Either[ConnectorError, SubscriptionSuccess]] =
      TestSubscriptionOrchestrationService.createSubscriptionFromUnauthorisedAgent(testArn, testNino, testSummaryData)

    "return a success when all incometax.business.services succeed" in {
      mockCreateSubscriptionSuccess(testNino, testSummaryData, Some(testArn))
      mockAddKnownFactsSuccess(testMTDID, testNino)
      mockEnrolSuccess(testMTDID, testNino)

      whenReady(res)(_ mustBe testSubscriptionSuccess)
    }

    "return a failure" when {
      "create subscription returns an error" in {
        mockCreateSubscriptionFailure(testNino, testSummaryData, Some(testArn))

        whenReady(res)(_ mustBe testSubscriptionFailure)
      }

      "create subscription returns an exception" in {
        mockCreateSubscriptionException(testNino, testSummaryData, Some(testArn))

        whenReady(res.failed)(_ mustBe testException)
      }

      "add known facts returns an error" in {
        mockCreateSubscriptionSuccess(testNino, testSummaryData, Some(testArn))
        mockAddKnownFactsFailure(testMTDID, testNino)

        whenReady(res)(_ mustBe testKnownFactsFailure)
      }

      "add known facts returns an exception" in {
        mockCreateSubscriptionSuccess(testNino, testSummaryData, Some(testArn))
        mockAddKnownFactsException(testMTDID, testNino)

        whenReady(res.failed)(_ mustBe testException)
      }
    }
  }

  "enrolAndRefresh" should {
    def res: Future[Either[ConnectorError, String]] =
      TestSubscriptionOrchestrationService.enrolAndRefresh(testMTDID, testNino)

    "return a success when enrolment and refresh profile succeed" in {
      mockEnrolSuccess(testMTDID, testNino)

      whenReady(res)(_ mustBe Right(testMTDID))
    }

    "return a failure" when {
      "enrol returns an error" in {
        mockEnrolFailure(testMTDID, testNino)

        whenReady(res)(_ mustBe testEnrolFailure)
      }

      "enrol returns an exception" in {
        mockEnrolException(testMTDID, testNino)

        whenReady(res.failed)(_ mustBe testException)
      }

    }
  }
}
