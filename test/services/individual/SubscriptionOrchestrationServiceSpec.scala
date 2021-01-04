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

package services.individual

import models.ConnectorError
import models.common.subscription.SubscriptionSuccess
import org.scalatest.concurrent.ScalaFutures
import services.individual.mocks.TestSubscriptionOrchestrationService
import utilities.TestModels._
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends UnitTestTrait with ScalaFutures
  with TestSubscriptionOrchestrationService {

  "createSubscription when ReleaseFour is disable" should {
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

  "createSubscription when ReleaseFour is enabled" should {
    def res: Future[Either[ConnectorError, SubscriptionSuccess]] =
      TestSubscriptionOrchestrationService.createSubscription(
        testNino,
        testIndividualSummary,
        isReleaseFourEnabled = true
      )

    "return a success when all incometax.business.services succeed" in {
      mockSignUpIncomeSourcesSuccess(testNino)
      mockCreateIncomeSourcesSuccess(testNino, testMTDID, testIndividualSummary)
      mockAddKnownFactsSuccess(testMTDID, testNino)
      mockEnrolSuccess(testMTDID, testNino)

      whenReady(res)(_ mustBe testSubscriptionSuccess)
    }

    "return a failure" when {
      "create income sources returns an error when sign up income sources request fail" in {
        mockSignUpIncomeSourcesFailure(testNino)

        whenReady(res)(_ mustBe testSignUpIncomeSourcesFailure)
      }

      "create income sources returns an error when create income sources request fail" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFailure(testNino, testMTDID, testIndividualSummary)

        whenReady(res)(_ mustBe testCreateIncomeSourcesFailure)
      }

      "create income sources returns an exception when sign up income sources throws an exception" in {
        mockSignUpIncomeSourcesException(testNino)

        whenReady(res.failed)(_ mustBe testException)
      }

      "create income sources returns an exception when create income sources throws an exception" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesException(testNino, testMTDID, testIndividualSummary)

        whenReady(res.failed)(_ mustBe testException)
      }

      "add known facts returns an error" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testIndividualSummary)
        mockAddKnownFactsFailure(testMTDID, testNino)

        whenReady(res)(_ mustBe testKnownFactsFailure)
      }

      "add known facts returns an exception" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testIndividualSummary)
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
