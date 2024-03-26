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

import com.github.tomakehurst.wiremock.http.Response.response
import common.Constants.ITSASessionKeys
import connectors.IncomeTaxSubscriptionConnector
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsResponse, PostSubscriptionDetailsSuccessResponse}
import models.{DateModel, Next}
import models.common.{AccountingYearModel, PropertyModel}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.Futures._
import org.scalatest.matchers.should.Matchers._
import play.api.test.FakeRequest
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataKeys._
import utilities.{SubscriptionDataKeys, UnitTestTrait}

import scala.concurrent.Future

class SubscriptionDetailsServiceSpec extends UnitTestTrait
  with MockSubscriptionDetailsService {

  override val mockConnector: IncomeTaxSubscriptionConnector = mock[IncomeTaxSubscriptionConnector]
  val subscriptionDetailsService: SubscriptionDetailsService = MockSubscriptionDetailsService
  val testReference = "test-reference"

  "mock Subscription Details  service" should {

    "return next year when the ELIGIBLE_NEXT_YEAR_ONLY session variable is set" in {
      val request = FakeRequest().withSession(
        ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "true"
      )
      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(request, mock[HeaderCarrier])
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe false
        testResult.get.confirmed mustBe true
        testResult.get.editable mustBe false
        testResult.get.accountingYear mustBe Next
      })
      verifySubscriptionDetailsFetchWithField(testReference, 0, SubscriptionDataKeys.SelectedTaxYear)
    }

    "return empty db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set and no value found" in {
      val request = FakeRequest()
      mockFetchSelectedTaxYear(None)
      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(request, mock[HeaderCarrier])
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe true
      })
      verifySubscriptionDetailsFetchWithField(testReference, 1, SubscriptionDataKeys.SelectedTaxYear)
    }

    "return editable db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set" in {
      val request = FakeRequest()
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Next, confirmed = true, editable = false)))
      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(request, mock[HeaderCarrier])
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
