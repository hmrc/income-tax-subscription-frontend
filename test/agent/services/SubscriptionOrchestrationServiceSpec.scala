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

package agent.services

import agent.utils.TestConstants._
import core.utils.TestModels.testSummaryData
import incometax.subscription.services.mocks.MockSubscriptionService
import models.individual.subscription.{SubscriptionFailure, SubscriptionSuccess}
import play.api.test.Helpers._
import services.AutoEnrolmentService
import services.mocks.MockAutoEnrolmentService

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends MockSubscriptionService with MockAutoEnrolmentService {

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockAutoEnrolmentService
  )

  "createSubscription" should {

    def res: Future[Either[SubscriptionFailure, SubscriptionSuccess]] = {
      TestSubscriptionOrchestrationService.createSubscription(testARN, testNino, testUtr, testSummaryData)
    }

    "return a success" when {
      "all services succeed" in {
        mockCreateSubscriptionSuccess(testNino, testSummaryData, testARN)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)

        await(res) mustBe testSubscriptionSuccess
      }
      "the auto enrolment service returns a failure response" in {
        mockCreateSubscriptionSuccess(testNino, testSummaryData, testARN)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.NoUsersFound)

        await(res) mustBe testSubscriptionSuccess
      }
    }

    "return a failure" when {
      "create subscription returns an error" in {
        mockCreateSubscriptionFailure(testNino, testSummaryData, testARN)

        await(res) mustBe testSubscriptionFailure
      }

    }
  }

}
