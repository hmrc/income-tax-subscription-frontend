/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.test.Helpers._
import services.mocks.{MockAutoEnrolmentService, MockSubscriptionService}
import utilities.TestModels.testAgentSummaryData
import utilities.agent.TestConstants._

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends MockSubscriptionService with MockAutoEnrolmentService {

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockAutoEnrolmentService
  )

  "createSubscription when release four is disabled" should {

    def res: Future[Either[ConnectorError, SubscriptionSuccess]] = {
      TestSubscriptionOrchestrationService.createSubscription(testARN, testNino, testUtr, testAgentSummaryData)
    }

    "return a success" when {
      "all services succeed" in {
        mockCreateSubscriptionSuccess(testNino, testAgentSummaryData, testARN)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)

        await(res) mustBe testSubscriptionSuccess
      }
      "the auto enrolment service returns a failure response" in {
        mockCreateSubscriptionSuccess(testNino, testAgentSummaryData, testARN)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.NoUsersFound)

        await(res) mustBe testSubscriptionSuccess
      }
    }

    "return a failure" when {
      "create subscription returns an error" in {
        mockCreateSubscriptionFailure(testNino, testAgentSummaryData, testARN)

        await(res) mustBe testSubscriptionFailure
      }

    }
  }

  "createSubscription when release four is enabled" should {

    def res: Future[Either[ConnectorError, SubscriptionSuccess]] = {
      TestSubscriptionOrchestrationService.createSubscription(testARN, testNino, testUtr, testAgentSummaryData, isReleaseFourEnabled = true)
    }

    "return a success" when {
      "all services succeed" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testAgentSummaryData, isPropertyNextTaxYearEnabled = false)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)

        await(res) mustBe testSubscriptionSuccess
      }
    }

    "return a failure" when {
      "create income sources returns an error when sign up income sources request fail" in {
        mockSignUpIncomeSourcesFailure(testNino)

        await(res) mustBe testSignUpIncomeSourcesFailure

      }

      "create income sources returns an error when create income sources request fail" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFailure(testNino, testMTDID, testAgentSummaryData, isPropertyNextTaxYearEnabled = false)

        await(res) mustBe testCreateIncomeSourcesFailure
      }

      "the auto enrolment service returns a failure response" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testAgentSummaryData, isPropertyNextTaxYearEnabled = false)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.NoUsersFound)

        await(res) mustBe Right(SubscriptionSuccess(testMTDID))
      }


    }
  }
}
