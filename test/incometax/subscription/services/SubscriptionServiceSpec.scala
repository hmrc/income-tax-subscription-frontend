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

package incometax.subscription.services

import core.models.Cash
import core.utils.TestConstants._
import core.utils.TestModels._
import core.utils.{TestConstants, TestModels}
import incometax.subscription.models._
import incometax.subscription.services.mocks.TestSubscriptionService
import incometax.util.AccountingPeriodUtil
import org.scalatest.EitherValues
import org.scalatest.Matchers._
import play.api.test.Helpers._


class SubscriptionServiceSpec extends TestSubscriptionService
  with EitherValues {

  val testNino: String = TestConstants.testNino


  "subscriptionService.buildRequestV2" should {
    "convert the user's data into the correct format when they own a property and self employed" in {
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequestV2(nino, testSummaryData, None)

      request.nino mustBe nino
      request.businessIncome.get.accountingPeriod.startDate mustBe testAccountingPeriod.startDate
      request.businessIncome.get.accountingPeriod.endDate mustBe testAccountingPeriod.endDate
      request.businessIncome.get.accountingMethod mustBe Cash
      request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
      request.propertyIncome.isDefined mustBe true
      request.isAgent mustBe false
    }

    "convert the user's data into the correct format when they own a property" in {
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequestV2(nino, testSummaryDataProperty, None)

      request.nino mustBe nino
      request.businessIncome.isDefined mustBe false
      request.propertyIncome.isDefined mustBe true
      request.isAgent mustBe false
    }

    "convert the user's data into the correct format when they are self employed" in {
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequestV2(nino, testSummaryDataBusiness, None)

      request.nino mustBe nino
      request.businessIncome.get.accountingPeriod.startDate mustBe testAccountingPeriod.startDate
      request.businessIncome.get.accountingPeriod.endDate mustBe testAccountingPeriod.endDate
      request.businessIncome.get.accountingMethod mustBe Cash
      request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
      request.propertyIncome.isDefined mustBe false
      request.isAgent mustBe false
    }

    "convert the user's data into the correct format when they are self employed and they are signing up in next Tax year" in {
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequestV2(nino, testSummaryDataBusinessMatchTaxYear.copy(selectedTaxYear = Some(testSelectedTaxYearNext)), None)

      val expectedTaxYear = AccountingPeriodUtil.getNextTaxYear

      request.nino mustBe nino
      request.businessIncome.get.accountingPeriod.startDate mustBe expectedTaxYear.startDate
      request.businessIncome.get.accountingPeriod.endDate mustBe expectedTaxYear.endDate
      request.businessIncome.get.accountingMethod mustBe Cash
      request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
      request.propertyIncome.isDefined mustBe false
      request.isAgent mustBe false

    }
  }

  "SubscriptionService.submitSubscription" should {

      def call = await(TestSubscriptionService.submitSubscription(nino = testNino, summaryData = testSummaryData, arn = None))

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
