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

import agent.connectors.models.subscription.{BadlyFormattedSubscriptionResponse, IncomeSourceType, SubscriptionFailureResponse, SubscriptionSuccess}
import org.scalatest.EitherValues
import org.scalatest.Matchers._
import play.api.test.Helpers._
import agent.services.mocks.TestSubscriptionService
import agent.utils.TestConstants._
import agent.utils.TestModels._
import agent.utils.{TestConstants, TestModels}


class SubscriptionServiceSpec extends TestSubscriptionService with EitherValues {

  val testNino: String = TestConstants.testNino
  val testArn: String = TestConstants.testARN

  "SubscriptionService.buildRequest" should {
    "convert the user's data into the correct FERequest format" in {
      // a freshly generated nino is used to ensure it is not simply pulling the test nino from somewhere else
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequest(testArn, nino, testSummaryData)
      request.nino mustBe nino
      request.accountingPeriodStart.get mustBe testSummaryData.accountingPeriod.get.startDate
      request.accountingPeriodEnd.get mustBe testSummaryData.accountingPeriod.get.endDate
      request.cashOrAccruals.get mustBe testSummaryData.accountingMethod.get.accountingMethod
      IncomeSourceType.unapply(request.incomeSource).get mustBe testSummaryData.incomeSource.get.source
      request.isAgent mustBe false
      request.tradingName.get mustBe testSummaryData.businessName.get.businessName
    }
  }

  "SubscriptionService.submitSubscription" should {
    def call = await(TestSubscriptionService.submitSubscription(arn = testArn, nino = testNino, summaryData = testSummaryData))

    "return the safeId when the subscription is successful" in {
      setupMockSubscribeSuccess(testSubmissionRequest)
      call.right.value shouldBe SubscriptionSuccess(testMTDID)
    }

    "return the error if subscription fails on bad request" in {
      setupMockSubscribeFailure(testSubmissionRequest)
      call.left.value shouldBe SubscriptionFailureResponse(BAD_REQUEST)
    }

    "return the error if subscription fails on bad formatting" in {
      setupMockSubscribeBadFormatting(testSubmissionRequest)
      call.left.value shouldBe BadlyFormattedSubscriptionResponse
    }

    "return the error if subscription throws an exception" in {
      setupMockSubscribeException(testSubmissionRequest)
      intercept[Exception](call) shouldBe testException

    }
  }

  "SubscriptionService.getSubscription" should {

    def call = await(TestSubscriptionService.getSubscription(nino = testNino))

    "return the safeId when the subscription is returned" in {
      setupMockGetSubscriptionFound(testNino)
      call.right.value shouldBe Some(SubscriptionSuccess(testMTDID))
    }

    "return the None when the subscription is returned as None" in {
      setupMockGetSubscriptionNotFound(testNino)
      call.right.value shouldBe empty
    }

    "return the error if subscription fails on bad request" in {
      setupMockGetSubscriptionFailure(testNino)
      call.left.value shouldBe SubscriptionFailureResponse(BAD_REQUEST)
    }

    "return the error if subscription throws an exception" in {
      setupMockGetSubscriptionException(testNino)
      intercept[Exception](call) shouldBe testException
    }
  }

}
