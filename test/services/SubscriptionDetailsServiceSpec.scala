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

import common.Constants.ITSASessionKeys
import connectors.IncomeTaxSubscriptionConnector
import models.Next
import models.common.AccountingYearModel
import models.status.MandationStatus.Voluntary
import org.scalatest.matchers.should.Matchers._
import play.api.test.FakeRequest
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.TestConstants.{testNino, testUtr}
import utilities.{SubscriptionDataKeys, UnitTestTrait}

class SubscriptionDetailsServiceSpec extends UnitTestTrait
  with MockSubscriptionDetailsService {

  override val mockConnector: IncomeTaxSubscriptionConnector = mock[IncomeTaxSubscriptionConnector]
  val subscriptionDetailsService: SubscriptionDetailsService = MockSubscriptionDetailsService
  val testReference = "test-reference"

  "mock Subscription Details  service" should {

    "return next year when the ELIGIBLE_NEXT_YEAR_ONLY session variable is set" in {
      mockGetMandationService(testNino, testUtr)(Voluntary, Voluntary)
      mockFetchSelectedTaxYear(None)

      val request = FakeRequest().withSession(
        ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "true"
      )
      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference, testNino, testUtr)(request, mock[HeaderCarrier])
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe false
        testResult.get.confirmed mustBe true
        testResult.get.editable mustBe false
        testResult.get.accountingYear mustBe Next
      })
    }

    "return empty db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set and no value found" in {
      mockGetMandationService(testNino, testUtr)(Voluntary, Voluntary)

      val request = FakeRequest()
      mockFetchSelectedTaxYear(None)
      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference, testNino, testUtr)(request, mock[HeaderCarrier])
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe true
      })
      verifySubscriptionDetailsFetchWithField(testReference, 1, SubscriptionDataKeys.SelectedTaxYear)
    }

    "return editable db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set" in {
      mockGetMandationService(testNino, testUtr)(Voluntary, Voluntary)

      val request = FakeRequest()
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Next, confirmed = true, editable = false)))
      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference, testNino, testUtr)(request, mock[HeaderCarrier])
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe false
        testResult.get.confirmed mustBe true
        testResult.get.editable mustBe true
        testResult.get.accountingYear mustBe Next
      })
      verifySubscriptionDetailsFetchWithField(testReference, 1, SubscriptionDataKeys.SelectedTaxYear)
    }
  }
}
