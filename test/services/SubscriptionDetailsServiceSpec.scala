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

import models.*
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.play.PlaySpec
import services.mocks.{MockGetEligibilityStatusService, MockIncomeTaxSubscriptionConnector, MockMandationStatusService, MockSessionDataService}
import uk.gov.hmrc.crypto.ApplicationCrypto
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

class SubscriptionDetailsServiceSpec extends PlaySpec
  with Matchers
  with MockMandationStatusService
  with MockIncomeTaxSubscriptionConnector
  with MockGetEligibilityStatusService
  with MockSessionDataService {

  val test: Map[String, String] = Map.empty

  val newTest: Map[String, String] = test ++ Some("" -> "")

  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]

  val subscriptionDetailsService: SubscriptionDetailsService = new SubscriptionDetailsService(
    mockIncomeTaxSubscriptionConnector,
    mockMandationStatusService,
    mockGetEligibilityStatusService,
    mockSessionDataService,
    crypto
  )

  val testReference = "test-reference"

  "mock Subscription Details service" should {
    "return next year when the ELIGIBLE_NEXT_YEAR_ONLY session variable is set" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(None)
      mockGetMandationService(Voluntary, Voluntary)
       mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))

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

    "return next year when both ELIGIBLE_NEXT_YEAR_ONLY and mandated current year are true" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(Some(AccountingYearModel(Current)))
      mockGetMandationService(Mandated, Mandated)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))

      val testResult = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)

      testResult.map { result =>
        result mustBe defined
        result.get.accountingYear mustBe Next
        result.get.confirmed mustBe true
        result.get.editable mustBe false
      }
    }

    "return current year when mandated current year is true and not eligible next year only" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(None)
      mockGetMandationService(Mandated, Mandated)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = false, exemptionReason = None))

      val testResult = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)

      testResult.map { result =>
        result mustBe defined
        result.get.accountingYear mustBe Current
        result.get.editable mustBe false
      }
    }
  }

  lazy val date: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

}
