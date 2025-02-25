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
import utilities.TestModels.testAccountingPeriodThisYear
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends UnitTestTrait with ScalaFutures
  with TestSubscriptionOrchestrationService {

  val testTaxYear: String = testAccountingPeriodThisYear.toLongTaxYear

  "signUpAndCreateIncomeSourcesFromTaskList" should {
    def res: Future[Either[ConnectorError, Option[SubscriptionSuccess]]] =
      TestSubscriptionOrchestrationService.signUpAndCreateIncomeSourcesFromTaskList(
        testCreateIncomeSources,
        testUtr
      )

    "return a success with an mtditid when all api calls were successful" in {
      mockSignUpSuccess(testNino, testUtr, testTaxYear)
      mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSources)
      mockAddKnownFactsSuccess(testMTDID, testNino)
      mockEnrolSuccess(testMTDID, testNino)

      await(res) mustBe testSubscriptionSuccess
    }

    "return a success without an mtditid when the sign up indicated the user was already signed up" in {
      mockAlreadySignedUp(testNino, testUtr, testTaxYear)

      await(res) mustBe Right(None)
    }

    "return a failure" when {
      "sign up income sources request fail and returns an error" in {
        mockSignUpIncomeSourcesFailure(testNino, testUtr, testTaxYear)

        await(res) mustBe testSignUpIncomeSourcesFailure

        verifyAddKnownFacts(testMTDID, testNino, 0)
      }

      "create income sources from task list request fail and returns an error" in {
        mockSignUpSuccess(testNino, testUtr, testTaxYear)
        mockAddKnownFactsSuccess(testMTDID, testNino)
        mockCreateIncomeSourcesFromTaskListFailure(testMTDID, testCreateIncomeSources)

        await(res) mustBe testCreateIncomeSourcesFromTaskListFailure

        verifyAddKnownFacts(testMTDID, testNino)
      }

      "sign up income sources throws an exception" in {
        mockSignUpIncomeSourcesException(testNino, testUtr, testTaxYear)

        await(res.failed) mustBe testException
      }

      "create income sources from task list throws an exception" in {
        mockSignUpSuccess(testNino, testUtr, testTaxYear)
        mockAddKnownFactsSuccess(testMTDID, testNino)

        mockCreateIncomeSourcesFromTaskListException(testMTDID, testCreateIncomeSources)

        await(res.failed) mustBe testException

        verifyAddKnownFacts(testMTDID, testNino)
      }

      "add known facts returns an error" in {
        mockSignUpSuccess(testNino, testUtr, testTaxYear)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSources)
        mockAddKnownFactsFailure(testMTDID, testNino)

        await(res) mustBe testKnownFactsFailure
      }

      "add known facts returns an exception" in {
        mockSignUpSuccess(testNino, testUtr, testTaxYear)
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
