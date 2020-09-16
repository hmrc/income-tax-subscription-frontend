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

package services

import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser.PostCreateIncomeSourceResponse
import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser.PostSignUpIncomeSourcesResponse
import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import models.Cash
import models.individual.subscription._
import org.scalatest.EitherValues
import org.scalatest.Matchers._
import play.api.test.Helpers._
import services.mocks.TestSubscriptionService
import utilities.AccountingPeriodUtil.getCurrentTaxYear
import utilities.TestModels._
import utilities.individual.TestConstants
import utilities.individual.TestConstants._
import utilities.{AccountingPeriodUtil, TestModels}


class SubscriptionServiceSpec extends TestSubscriptionService
  with EitherValues {

  val testNino: String = TestConstants.testNino


  "subscriptionService.buildRequestPost" when {
    "an agent" should {
      "covert the user's data into the correct format when they own a property and self employed" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testAgentSummaryDataBoth, Some(testArn))

        request.nino mustBe nino
        request.businessIncome.get.accountingPeriod.startDate mustBe testAccountingPeriod.startDate
        request.businessIncome.get.accountingPeriod.endDate mustBe testAccountingPeriod.endDate
        request.businessIncome.get.accountingMethod mustBe Cash
        request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
        request.propertyIncome.isDefined mustBe true
        request.isAgent mustBe true
      }

      "convert the user's data into the correct format when they own a property" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testAgentSummaryDataProperty, Some(testArn))

        request.nino mustBe nino
        request.businessIncome.isDefined mustBe false
        request.propertyIncome.get.accountingMethod.get mustBe Cash
        request.isAgent mustBe true
      }

      "convert the user's data into the correct format when they are self employed" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testAgentSummaryDataBusiness, Some(testArn))

        request.nino mustBe nino
        request.businessIncome.get.accountingPeriod.startDate mustBe testAccountingPeriod.startDate
        request.businessIncome.get.accountingPeriod.endDate mustBe testAccountingPeriod.endDate
        request.businessIncome.get.accountingMethod mustBe Cash
        request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
        request.propertyIncome.isDefined mustBe false
        request.isAgent mustBe true
      }

      "convert the user's data into the correct format when they are self employed and they are signing up in next Tax year" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testAgentSummaryDataBusiness.copy(selectedTaxYear =
          Some(testSelectedTaxYearNext)), Some(testArn))

        request.nino mustBe nino
        request.businessIncome.get.accountingPeriod.startDate mustBe testAccountingPeriod.startDate
        request.businessIncome.get.accountingPeriod.endDate mustBe testAccountingPeriod.endDate
        request.businessIncome.get.accountingMethod mustBe Cash
        request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
        request.propertyIncome.isDefined mustBe false
        request.isAgent mustBe true
      }
    }
    "an individual" should {
      "convert the user's data into the correct format when they own a property and self employed" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testSummaryData, None)

        val expectedTaxYear = AccountingPeriodUtil.getCurrentTaxYear

        request.nino mustBe nino
        request.businessIncome.get.accountingPeriod.startDate mustBe expectedTaxYear.startDate
        request.businessIncome.get.accountingPeriod.endDate mustBe expectedTaxYear.endDate
        request.businessIncome.get.accountingMethod mustBe Cash
        request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
        request.propertyIncome.isDefined mustBe true
        request.isAgent mustBe false
      }

      "convert the user's data into the correct format when they own a property" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testSummaryDataProperty, None)

        request.nino mustBe nino
        request.businessIncome.isDefined mustBe false
        request.propertyIncome.isDefined mustBe true
        request.isAgent mustBe false
      }

      "convert the user's data into the correct format when they are self employed" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testSummaryDataBusiness, None)

        val expectedTaxYear = AccountingPeriodUtil.getCurrentTaxYear

        request.nino mustBe nino
        request.businessIncome.get.accountingPeriod.startDate mustBe expectedTaxYear.startDate
        request.businessIncome.get.accountingPeriod.endDate mustBe expectedTaxYear.endDate
        request.businessIncome.get.accountingMethod mustBe Cash
        request.businessIncome.get.tradingName.get mustBe testBusinessName.businessName
        request.propertyIncome.isDefined mustBe false
        request.isAgent mustBe false
      }

      "convert the user's data into the correct format when they are self employed and they are signing up in next Tax year" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequestPost(nino, testSummaryDataBusinessMatchTaxYear.copy(selectedTaxYear =
          Some(testSelectedTaxYearNext)), None)

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
  }

  "SubscriptionService.submitSubscription" should {

      def call: SubscriptionResponse = await(TestSubscriptionService.submitSubscription(nino = testNino, summaryData = testSummaryData, arn = None))

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

    def call: GetSubscriptionResponse = await(TestSubscriptionService.getSubscription(nino = testNino))

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

  "SubscriptionService.signUpIncomeSources" should {

    def call: PostSignUpIncomeSourcesResponse = await(TestSubscriptionService.signUpIncomeSources(nino = testNino))

    "return the mtdbsa id when the signUp is successful" in {
      setupMockSignUpIncomeSourcesSuccess(testNino)
      call.right.value shouldBe SignUpIncomeSourcesSuccess(testMTDID)
    }

    "return the error if sign up fails on bad request" in {
      setupMockSignUpIncomeSourcesFailure(testNino)
      call.left.value shouldBe SignUpIncomeSourcesFailureResponse(BAD_REQUEST)
    }

    "return the error if sign up fails on bad formatting" in {
      setupMockSignUpIncomeSourcesBadFormatting(testNino)
      call.left.value shouldBe BadlyFormattedSignUpIncomeSourcesResponse
    }

    "return the error if subscription throws an exception" in {
      setupMockSignUpIncomeSourcesException(testNino)
      intercept[Exception](call) shouldBe testException
    }
  }

  "SubscriptionService.createIncomeSources" should {

    def call: PostCreateIncomeSourceResponse = await(TestSubscriptionService.createIncomeSources(mtdbsa = testMTDID, testIndividualSummary))

    "return the list of income source ids when the create is successful" in {
      setupMockCreateIncomeSourcesSuccess(testMTDID,
        testIndividualSummary.toBusinessSubscriptionDetailsModel.copy(accountingPeriod = getCurrentTaxYear))
      call.right.value shouldBe CreateIncomeSourcesSuccess()
    }

    "return the error if create fails on bad request" in {
      setupMockCreateIncomeSourcesFailure(testMTDID,
        testIndividualSummary.toBusinessSubscriptionDetailsModel.copy(accountingPeriod = getCurrentTaxYear))
      call.left.value shouldBe CreateIncomeSourcesFailureResponse(BAD_REQUEST)
    }

    "return the error if create fails on bad formatting" in {
      setupMockCreateIncomeSourcesBadFormatting(testMTDID,
        testIndividualSummary.toBusinessSubscriptionDetailsModel.copy(accountingPeriod = getCurrentTaxYear))
      call.left.value shouldBe BadlyFormattedCreateIncomeSourcesResponse
    }

    "return the error if subscription throws an exception" in {
      setupMockCreateIncomeSourcesException(testMTDID,
        testIndividualSummary.toBusinessSubscriptionDetailsModel.copy(accountingPeriod = getCurrentTaxYear))
      intercept[Exception](call) shouldBe testException
    }
  }
}
