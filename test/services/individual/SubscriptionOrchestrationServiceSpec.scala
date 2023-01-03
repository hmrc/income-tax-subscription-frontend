/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.individual.mocks.TestSubscriptionOrchestrationService
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends UnitTestTrait with ScalaFutures
  with TestSubscriptionOrchestrationService {

  "signUpAndCreateIncomeSourcesFromTaskList" should {
    def res: Future[Either[ConnectorError, SubscriptionSuccess]] =
      TestSubscriptionOrchestrationService.signUpAndCreateIncomeSourcesFromTaskList(
        testNino,
        testCreateIncomeSources
      )

    "return a success when all incometax.business.services succeed" in {
      mockSignUpIncomeSourcesSuccess(testNino)
      mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSources)
      mockAddKnownFactsSuccess(testMTDID, testNino)
      mockEnrolSuccess(testMTDID, testNino)

      await(res) mustBe testSubscriptionSuccess
    }

    "return a failure" when {
      "sign up income sources request fail and returns an error" in {
        mockSignUpIncomeSourcesFailure(testNino)

        await(res) mustBe testSignUpIncomeSourcesFailure
      }

      "create income sources from task list request fail and returns an error" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFromTaskListFailure(testMTDID, testCreateIncomeSources)

        await(res) mustBe testCreateIncomeSourcesFromTaskListFailure
      }

      "sign up income sources throws an exception" in {
        mockSignUpIncomeSourcesException(testNino)

        await(res.failed) mustBe testException
      }

      "create income sources from task list throws an exception" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFromTaskListException(testMTDID, testCreateIncomeSources)

        await(res.failed) mustBe testException
      }

      "add known facts returns an error" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSources)
        mockAddKnownFactsFailure(testMTDID, testNino)

        await(res) mustBe testKnownFactsFailure
      }

      "add known facts returns an exception" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSources)
        mockAddKnownFactsException(testMTDID, testNino)

        await(res.failed) mustBe testException
      }
    }


    "enrolAndRefresh" should {
      def res: Future[Either[ConnectorError, String]] =
        TestSubscriptionOrchestrationService.enrolAndRefresh(testMTDID, testNino)

      "return a success when enrolment and refresh profile succeed" in {
        mockEnrolSuccess(testMTDID, testNino)

        await(res) mustBe Right(testMTDID)
      }

      "return a failure" when {
        "enrol returns an error" in {
          mockEnrolFailure(testMTDID, testNino)

          await(res) mustBe testEnrolFailure
        }

        "enrol returns an exception" in {
          mockEnrolException(testMTDID, testNino)

          await(res.failed) mustBe testException
        }

      }
    }
  }
}
