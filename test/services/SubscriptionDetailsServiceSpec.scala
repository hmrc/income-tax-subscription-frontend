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

package services

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.httpparser.{DeleteSubscriptionDetailsHttpParser, PostSubscriptionDetailsHttpParser}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsResponse, PostSubscriptionDetailsSuccessResponse}
import models._
import models.common.{AccountingYearModel, PropertyModel}
import models.status.MandationStatus.Voluntary
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.{MockGetEligibilityStatusService, MockIncomeTaxSubscriptionConnector, MockMandationStatusService}
import uk.gov.hmrc.crypto.ApplicationCrypto
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

import scala.concurrent.Future

class SubscriptionDetailsServiceSpec extends PlaySpec
  with Matchers
  with MockMandationStatusService
  with MockIncomeTaxSubscriptionConnector
  with MockGetEligibilityStatusService {

  val test: Map[String, String] = Map.empty

  val newTest: Map[String, String] = test ++ Some("" -> "")

  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]

  val subscriptionDetailsService: SubscriptionDetailsService = new SubscriptionDetailsService(
    mockIncomeTaxSubscriptionConnector,
    mockMandationStatusService,
    mockGetEligibilityStatusService,
    crypto
  )

  val testReference = "test-reference"

  "mock Subscription Details  service" should {

    "return next year when the ELIGIBLE_NEXT_YEAR_ONLY session variable is set" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(None)
      mockGetMandationService(Voluntary, Voluntary)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))

      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe false
        testResult.get.confirmed mustBe true
        testResult.get.editable mustBe false
        testResult.get.accountingYear mustBe Next
      })
    }

    "return empty db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set and no value found" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(None)
      mockGetMandationService(Voluntary, Voluntary)


      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe true
      })
    }

    "return editable db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(Some(AccountingYearModel(Current)))
      mockGetMandationService(Voluntary, Voluntary)

      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe false
        testResult.get.confirmed mustBe true
        testResult.get.editable mustBe true
        testResult.get.accountingYear mustBe Next
      })
    }
  }

  "saveStreamlinedProperty" should {
    "return a save success" when {
      "no property data was fetched" when {
        "start date is provided, start date before limit flag is not" in {
          mockGetSubscriptionDetails(SubscriptionDataKeys.Property)(None)
          mockSaveSubscriptionDetails(SubscriptionDataKeys.Property, PropertyModel(
            accountingMethod = Some(Cash),
            startDate = Some(date)
          ))(Right(PostSubscriptionDetailsSuccessResponse))
          mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(Right(DeleteSubscriptionDetailsSuccessResponse))

          val result: Future[PostSubscriptionDetailsResponse] = subscriptionDetailsService.saveStreamlineProperty(
            reference = testReference,
            maybeStartDate = Some(date),
            maybeStartDateBeforeLimit = None,
            accountingMethod = Cash
          )

          await(result) mustBe Right(PostSubscriptionDetailsSuccessResponse)
        }
        "start date is not provided, start date before limit flag is" in {
          mockGetSubscriptionDetails(SubscriptionDataKeys.Property)(None)
          mockSaveSubscriptionDetails(SubscriptionDataKeys.Property, PropertyModel(
            startDateBeforeLimit = Some(false),
            accountingMethod = Some(Cash)
          ))(Right(PostSubscriptionDetailsSuccessResponse))
          mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(Right(DeleteSubscriptionDetailsSuccessResponse))

          val result: Future[PostSubscriptionDetailsResponse] = subscriptionDetailsService.saveStreamlineProperty(
            reference = testReference,
            maybeStartDate = None,
            maybeStartDateBeforeLimit = Some(false),
            accountingMethod = Cash
          )

          await(result) mustBe Right(PostSubscriptionDetailsSuccessResponse)
        }
      }
      "property data was fetched" in {
        mockGetSubscriptionDetails(SubscriptionDataKeys.Property)(Some(PropertyModel(
          startDateBeforeLimit = None,
          accountingMethod = Some(Accruals),
          startDate = Some(date),
          confirmed = true
        )))
        mockSaveSubscriptionDetails(SubscriptionDataKeys.Property, PropertyModel(
          accountingMethod = Some(Cash),
          startDate = Some(date)
        ))(Right(PostSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(Right(DeleteSubscriptionDetailsSuccessResponse))

        val result: Future[PostSubscriptionDetailsResponse] = subscriptionDetailsService.saveStreamlineProperty(
          reference = testReference,
          maybeStartDate = Some(date),
          maybeStartDateBeforeLimit = Some(false),
          accountingMethod = Cash
        )

        await(result) mustBe Right(PostSubscriptionDetailsSuccessResponse)
      }
    }
    "return a save failure" when {
      "there was a problem saving the property details" in {
        mockGetSubscriptionDetails(SubscriptionDataKeys.Property)(None)
        mockSaveSubscriptionDetails(SubscriptionDataKeys.Property, PropertyModel(
          accountingMethod = Some(Cash),
          startDate = Some(date)
        ))(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(Right(DeleteSubscriptionDetailsSuccessResponse))

        val result: Future[PostSubscriptionDetailsResponse] = subscriptionDetailsService.saveStreamlineProperty(
          reference = testReference,
          maybeStartDate = Some(date),
          maybeStartDateBeforeLimit = None,
          accountingMethod = Cash
        )

        await(result) mustBe Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
      "there was a problem deleting the income source confirmation" in {
        mockGetSubscriptionDetails(SubscriptionDataKeys.Property)(None)
        mockSaveSubscriptionDetails(SubscriptionDataKeys.Property, PropertyModel(
          accountingMethod = Some(Cash),
          startDate = Some(date)
        ))(Right(PostSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(
          Left(DeleteSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val result: Future[PostSubscriptionDetailsResponse] = subscriptionDetailsService.saveStreamlineProperty(
          reference = testReference,
          maybeStartDate = Some(date),
          maybeStartDateBeforeLimit = None,
          accountingMethod = Cash
        )

        await(result) mustBe Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }

  lazy val date: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

}
