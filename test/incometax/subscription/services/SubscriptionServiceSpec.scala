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

import core.config.featureswitch.{FeatureSwitching, UseSubscriptionApiV2}
import core.models.{Cash, No}
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
  with EitherValues
  with FeatureSwitching {

  val testNino: String = TestConstants.testNino

  "SubscriptionService.buildRequest" should {
    "convert the user's data into the correct FERequest format" in {
      // a freshly generated nino is used to ensure it is not simply pulling the test nino from somewhere else
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequest(nino, testSummaryData, None)
      request.nino mustBe nino
      request.accountingPeriodStart.get mustBe testSummaryData.accountingPeriod.get.startDate
      request.accountingPeriodEnd.get mustBe testSummaryData.accountingPeriod.get.endDate
      request.cashOrAccruals.get mustBe testSummaryData.accountingMethod.get.accountingMethod
      request.incomeSource mustBe Both
      request.isAgent mustBe false
      request.tradingName.get mustBe testSummaryData.businessName.get.businessName
    }

    "use the current tax year and ignore the accounting period dates if match tax year is answered yes" in {
      val nino = TestModels.newNino
      val request = TestSubscriptionService.buildRequest(nino, testSummaryData.copy(matchTaxYear = testMatchTaxYearYes), None)
      request.nino mustBe nino
      request.accountingPeriodStart.get must not be testSummaryData.accountingPeriod.get.startDate
      request.accountingPeriodStart.get mustBe AccountingPeriodUtil.getCurrentTaxYearStartDate
      request.accountingPeriodEnd.get must not be testSummaryData.accountingPeriod.get.endDate
      request.accountingPeriodEnd.get mustBe AccountingPeriodUtil.getCurrentTaxYearEndDate
      request.cashOrAccruals.get mustBe testSummaryData.accountingMethod.get.accountingMethod
      request.incomeSource mustBe Both
      request.isAgent mustBe false
      request.tradingName.get mustBe testSummaryData.businessName.get.businessName
    }

    "property requests should copy None into start and end dates" in {
      val nino = TestModels.newNino
      val testSummaryData = IndividualSummary(
        rentUkProperty = testRentUkProperty_property_only,
        areYouSelfEmployed = None,
        otherIncome = No
      )
      val request = TestSubscriptionService.buildRequest(nino, testSummaryData, None)
      request.nino mustBe nino
      request.accountingPeriodStart mustBe None
      request.accountingPeriodEnd mustBe None
      request.cashOrAccruals mustBe None
      request.incomeSource mustBe Property
      request.isAgent mustBe false
      request.tradingName mustBe None
    }
  }

  "subscriptionService.buildRequestV2" should {
    "convert the user's data into the correct format" in {
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
  }

  "SubscriptionService.submitSubscription" when {
    "the UseSubscriptionApiV2 feature switch is enabled" should {
      enable(UseSubscriptionApiV2)

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
    "the UseSubscriptionApiV2 feature switch is disabled" should {
      disable(UseSubscriptionApiV2)

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
