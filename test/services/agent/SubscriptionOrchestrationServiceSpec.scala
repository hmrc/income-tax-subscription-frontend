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

package services.agent

import models.ConnectorError
import models.common.subscription.SubscriptionSuccess
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.test.Helpers._
import services.agent.mocks.MockAgentSPSConnector
import services.mocks.{MockAutoEnrolmentService, MockSubscriptionService}
import utilities.TestModels.testAccountingPeriodThisYear
import utilities.agent.TestConstants._

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends MockSubscriptionService with MockAutoEnrolmentService with MockAgentSPSConnector {

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockAutoEnrolmentService,
    mockAgentSpsConnector
  )

  val testTaxYear: String = testAccountingPeriodThisYear.toLongTaxYear

  "createSubscriptionFromTaskList" should {
    def res: Future[Either[ConnectorError, SubscriptionSuccess]] = {
      TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testNino, testUtr, testCreateIncomeSourcesThisYear)
    }

    "return a success" when {
      "all services succeed" in {
        mockSignUpIncomeSourcesSuccess(testNino, testTaxYear)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSourcesThisYear)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(Right(AutoEnrolmentService.EnrolmentAssigned))
        mockAgentSpsConnectorSuccess(testARN, testUtr, testNino, testMTDID)

        val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testNino, testUtr, testCreateIncomeSourcesThisYear)

        await(res) mustBe testSubscriptionSuccess
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
      }
    }

    "return a failure" when {
      "sign up income sources request fail and returns an error" in {
        mockSignUpIncomeSourcesFailure(testNino, testTaxYear)

        whenReady(res)(_ mustBe testSignUpIncomeSourcesFailure)
      }

      "create income sources from task list request fail and returns an error" in {
        mockSignUpIncomeSourcesSuccess(testNino, testTaxYear)
        mockCreateIncomeSourcesFromTaskListFailure(testMTDID, testCreateIncomeSourcesThisYear)

        whenReady(res)(_ mustBe testCreateSubscriptionFromTaskListFailure)
      }
    }
  }
}
