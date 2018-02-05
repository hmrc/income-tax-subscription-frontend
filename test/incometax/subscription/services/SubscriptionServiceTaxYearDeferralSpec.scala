/*
 * Copyright 2018 HM Revenue & Customs
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

import core.config.featureswitch.{FeatureSwitching, NewIncomeSourceFlowFeature, TaxYearDeferralFeature}
import core.utils.TestConstants._
import core.utils.TestModels._
import core.utils.{TestConstants, TestModels}
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}
import incometax.subscription.models._
import incometax.subscription.services.mocks.TestSubscriptionService
import incometax.util.AccountingPeriodUtil
import incometax.util.AccountingPeriodUtil.getCurrentTaxYear
import org.scalatest.EitherValues
import org.scalatest.Matchers._
import play.api.test.Helpers._


class SubscriptionServiceTaxYearDeferralSpec extends TestSubscriptionService
  with EitherValues
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(TaxYearDeferralFeature)
    disable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(TaxYearDeferralFeature)
    disable(NewIncomeSourceFlowFeature)
  }

  val testNino: String = TestConstants.testNino

  "SubscriptionService.getIncomeSourceType" when {
    "NewIncomeSourceFlowFeature is disabled" when {
      "no arn is supplied" should {
        "return the correct income source type" in {
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceBusiness), None) mustBe Business
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceProperty), None) mustBe Property
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceBoth), None) mustBe Both
        }
      }
      "arn is supplied" should {
        "return the correct income source type" in {
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceBusiness), Some(testArn)) mustBe Business
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceProperty), Some(testArn)) mustBe Property
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceBoth), Some(testArn)) mustBe Both
        }
      }
    }
    "NewIncomeSourceFlowFeature is enabled" when {
      "no arn is supplied" should {
        "return the correct income source type" in {
          enable(NewIncomeSourceFlowFeature)
          TestSubscriptionService.getIncomeSourceType(
            testSummaryNewIncomeSourceData.copy(rentUkProperty = testRentUkProperty_no_property, workForYourself = testWorkForYourself_yes),
            None
          ) mustBe Business
          TestSubscriptionService.getIncomeSourceType(
            testSummaryNewIncomeSourceData.copy(rentUkProperty = testRentUkProperty_property_and_other, workForYourself = testWorkForYourself_no),
            None
          ) mustBe Property
          TestSubscriptionService.getIncomeSourceType(
            testSummaryNewIncomeSourceData.copy(rentUkProperty = testRentUkProperty_property_and_other, workForYourself = testWorkForYourself_yes),
            None
          ) mustBe Both
        }
      }
      "arn is supplied" should {
        "return the correct income source type" in {
          enable(NewIncomeSourceFlowFeature)
          // agent flow still uses the original income source model
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceBusiness), Some(testArn)) mustBe Business
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceProperty), Some(testArn)) mustBe Property
          TestSubscriptionService.getIncomeSourceType(testSummaryData.copy(incomeSource = testIncomeSourceBoth), Some(testArn)) mustBe Both
        }
      }
    }
  }

  "SubscriptionService.getAccountingPeriod" when {
    "no arn is supplied" should {
      val matchedTaxYear = testSummaryData.copy(matchTaxYear = Some(testMatchTaxYearYes))
      "return the accounting period based on user input" in {
        TestSubscriptionService.getAccountingPeriod(Business, summaryData = matchedTaxYear, None) mustBe Some(getCurrentTaxYear)
        TestSubscriptionService.getAccountingPeriod(Business, summaryData = testSummaryData, None) mustBe testSummaryData.accountingPeriod
        TestSubscriptionService.getAccountingPeriod(Property, summaryData = testSummaryData, None) mustBe None
        TestSubscriptionService.getAccountingPeriod(Both, summaryData = matchedTaxYear, None) mustBe Some(getCurrentTaxYear)
        TestSubscriptionService.getAccountingPeriod(Both, summaryData = testSummaryData, None) mustBe testSummaryData.accountingPeriod
      }
    }
    "arn is supplied" should {
      "return the accounting period based on user input" in {
        TestSubscriptionService.getAccountingPeriod(Business, summaryData = testSummaryData, Some(testArn)) mustBe testSummaryData.accountingPeriod
        TestSubscriptionService.getAccountingPeriod(Property, summaryData = testSummaryData, Some(testArn)) mustBe None
        TestSubscriptionService.getAccountingPeriod(Both, summaryData = testSummaryData, Some(testArn)) mustBe testSummaryData.accountingPeriod
      }
    }
  }

  "SubscriptionService.buildRequest" when {
    "the tax year is before 2018 - 2019" when {
      "NewIncomeSourceFlowFeature is disabled" should {
        "convert the user's data into the correct FERequest format" in {
          // a freshly generated nino is used to ensure it is not simply pulling the test nino from somewhere else
          val nino = TestModels.newNino
          val request = TestSubscriptionService.buildRequest(nino, testSummaryData, None)
          request.nino mustBe nino
          request.accountingPeriodStart.get mustBe testSummaryData.accountingPeriod.map(_.adjustedTaxYear.startDate).get
          request.accountingPeriodEnd.get mustBe testSummaryData.accountingPeriod.map(_.adjustedTaxYear.endDate).get
          request.cashOrAccruals.get mustBe testSummaryData.accountingMethod.get.accountingMethod
          IncomeSourceType.unapply(request.incomeSource).get mustBe testSummaryData.incomeSource.get.source
          request.isAgent mustBe false
          request.tradingName.get mustBe testSummaryData.businessName.get.businessName
        }

        "property requests should copy None into start and end dates" in {
          val nino = TestModels.newNino
          val testSummaryData = SummaryModel(
            incomeSource = IncomeSourceModel(IncomeSourceForm.option_property),
            otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
          )
          val request = TestSubscriptionService.buildRequest(nino, testSummaryData, None)
          request.nino mustBe nino
          request.accountingPeriodStart mustBe None
          request.accountingPeriodEnd mustBe None
          request.cashOrAccruals mustBe None
          IncomeSourceType.unapply(request.incomeSource).get mustBe testSummaryData.incomeSource.get.source
          request.isAgent mustBe false
          request.tradingName mustBe None
        }
      }

      "NewIncomeSourceFlowFeature is enabled" should {
        "convert the user's data into the correct FERequest format" in {
          enable(NewIncomeSourceFlowFeature)

          // a freshly generated nino is used to ensure it is not simply pulling the test nino from somewhere else
          val nino = TestModels.newNino
          val request = TestSubscriptionService.buildRequest(nino, testSummaryNewIncomeSourceData, None)
          request.nino mustBe nino
          request.accountingPeriodStart.get mustBe testSummaryData.accountingPeriod.map(_.adjustedTaxYear.startDate).get
          request.accountingPeriodEnd.get mustBe testSummaryData.accountingPeriod.map(_.adjustedTaxYear.endDate).get
          request.cashOrAccruals.get mustBe testSummaryNewIncomeSourceData.accountingMethod.get.accountingMethod
          request.incomeSource mustBe Both
          request.isAgent mustBe false
          request.tradingName.get mustBe testSummaryNewIncomeSourceData.businessName.get.businessName
        }

        "convert the agent''s data into the correct FERequest format" in {
          enable(NewIncomeSourceFlowFeature)

          // a freshly generated nino is used to ensure it is not simply pulling the test nino from somewhere else
          val nino = TestModels.newNino
          // testSummaryData is used here because the agent's submission will be using the original models
          val request = TestSubscriptionService.buildRequest(nino, testSummaryData, Some("test arn value"))
          request.nino mustBe nino
          request.accountingPeriodStart.get mustBe testSummaryData.accountingPeriod.get.startDate
          request.accountingPeriodEnd.get mustBe testSummaryData.accountingPeriod.get.endDate
          request.cashOrAccruals.get mustBe testSummaryData.accountingMethod.get.accountingMethod
          IncomeSourceType.unapply(request.incomeSource).get mustBe testSummaryData.incomeSource.get.source
          request.isAgent mustBe false
          request.tradingName.get mustBe testSummaryData.businessName.get.businessName
        }

        "property requests should copy None into start and end dates" in {
          enable(NewIncomeSourceFlowFeature)

          val nino = TestModels.newNino
          val testSummaryData = SummaryModel(
            rentUkProperty = testRentUkProperty_property_and_other,
            workForYourself = testWorkForYourself_no,
            otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
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

        "agent's property requests should be converted correctly" in {
          enable(NewIncomeSourceFlowFeature)

          val nino = TestModels.newNino
          val testSummaryData = SummaryModel(
            incomeSource = IncomeSourceModel(IncomeSourceForm.option_property),
            otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
          )
          val request = TestSubscriptionService.buildRequest(nino, testSummaryData, Some("test arn value"))
          request.nino mustBe nino
          request.accountingPeriodStart mustBe None
          request.accountingPeriodEnd mustBe None
          request.cashOrAccruals mustBe None
          IncomeSourceType.unapply(request.incomeSource).get mustBe testSummaryData.incomeSource.get.source
          request.isAgent mustBe false
          request.tradingName mustBe None
        }
      }

      "use the current tax year and ignore the accounting period dates if match tax year is answered yes" in {
        val nino = TestModels.newNino
        val request = TestSubscriptionService.buildRequest(nino, testSummaryData.copy(matchTaxYear = testMatchTaxYearYes), None)
        request.nino mustBe nino
        request.accountingPeriodStart.get must not be testSummaryData.accountingPeriod.get.startDate
        request.accountingPeriodStart.get mustBe AccountingPeriodUtil.getCurrentTaxYear.adjustedTaxYear.startDate
        request.accountingPeriodEnd.get must not be testSummaryData.accountingPeriod.get.endDate
        request.accountingPeriodEnd.get mustBe AccountingPeriodUtil.getCurrentTaxYear.adjustedTaxYear.endDate
        request.cashOrAccruals.get mustBe testSummaryData.accountingMethod.get.accountingMethod
        IncomeSourceType.unapply(request.incomeSource).get mustBe testSummaryData.incomeSource.get.source
        request.isAgent mustBe false
        request.tradingName.get mustBe testSummaryData.businessName.get.businessName
      }

    }

  }

  "SubscriptionService.submitSubscription" should {
    def call = await(TestSubscriptionService.submitSubscription(nino = testNino, summaryData = testSummaryData, arn = None))

    "return the safeId when the subscription is successful" in {
      setupMockSubscribeSuccess(testAdjustedSubmissionRequest)
      call.right.value shouldBe SubscriptionSuccess(testMTDID)
    }

    "return the error if subscription fails on bad request" in {
      setupMockSubscribeFailure(testAdjustedSubmissionRequest)
      call.left.value shouldBe SubscriptionFailureResponse(BAD_REQUEST)
    }

    "return the error if subscription fails on bad formatting" in {
      setupMockSubscribeBadFormatting(testAdjustedSubmissionRequest)
      call.left.value shouldBe BadlyFormattedSubscriptionResponse
    }

    "return the error if subscription throws an exception" in {
      setupMockSubscribeException(testAdjustedSubmissionRequest)
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
