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

import connectors.models.subscription.{FESuccessResponse, IncomeSourceType}
import org.scalatest.Matchers._
import play.api.test.Helpers._
import services.mocks.MockSubscriptionService
import utils.{TestConstants, TestModels}


class SubscriptionServiceSpec extends MockSubscriptionService {

  val testNino: String = TestConstants.testNino

  "SubscriptionService.buildRequest" should {
    "convert the user's data into the correct FERequest format" in {
      // a freshly generated nino is used to ensure it is not simply pulling the test nino from somewhere else
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequest(nino, testSummaryData)
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
    val testRequest = TestSubscriptionService.buildRequest(testNino, testSummaryData)
    def call = await(TestSubscriptionService.submitSubscription(nino = testNino, summaryData = testSummaryData))

    "return the safeId when the subscription is successful" in {
      setupSubscribe(testRequest)(subscribeSuccess)
      val response = call.get
      response.isInstanceOf[FESuccessResponse] shouldBe true
      response.asInstanceOf[FESuccessResponse].mtditId shouldBe Some(testId)
    }

    "return the error if subscription fails on bad request" in {
      setupSubscribe(testRequest)(subscribeBadRequest)
      val response = call
      response shouldBe None
    }

    "return the error if subscription fails on internal server error" in {
      setupSubscribe(testRequest)(subscribeInternalServerError)
      val response = call
      response shouldBe None

    }
  }

  "SubscriptionService.getSubscription" should {

    def call = await(TestSubscriptionService.getSubscription(nino = testNino))

    "return the safeId when the subscription is returned" in {
      setupGetSubscription(testNino)(subscribeSuccess)
      val response = call.get
      response.isInstanceOf[FESuccessResponse] shouldBe true
      response.asInstanceOf[FESuccessResponse].mtditId shouldBe Some(testId)
    }

    "return the None when the subscription is returned as None" in {
      setupGetSubscription(testNino)(subscribeNone)
      val response = call.get
      response.isInstanceOf[FESuccessResponse] shouldBe true
      response.asInstanceOf[FESuccessResponse].mtditId shouldBe None
    }

    "return the error if subscription fails on bad request" in {
      setupGetSubscription(testNino)(subscribeBadRequest)
      val response = call
      response shouldBe None
    }

    "return the error if subscription fails on internal server error" in {
      setupGetSubscription(testNino)(subscribeInternalServerError)
      val response = call
      response shouldBe None
    }
  }

}
