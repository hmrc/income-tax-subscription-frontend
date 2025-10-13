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

import config.featureswitch.FeatureSwitch.SignalControlGatewayEligibility
import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import connectors.httpparser.{GetSessionDataHttpParser, SaveSessionDataHttpParser}
import models.EligibilityStatus
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.TestGetEligibilityStatusService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import utilities.HttpResult.HttpConnectorError

class GetEligibilityStatusServiceSpec extends PlaySpec with TestGetEligibilityStatusService with FeatureSwitching {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val appConfig: AppConfig = MockConfig

  val eligibilityStatus: EligibilityStatus = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)

  val testNino: String = "test-nino"
  val testUtr: String = "test-utr"

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(SignalControlGatewayEligibility)
  }

  "getEligibilityStatus" when {
    "the signal control gateway eligibility feature switch is enabled" must {
      "return the eligibility status from session" when {
        "available in session" in {
          enable(SignalControlGatewayEligibility)

          mockFetchEligibilityStatus(Right(Some(eligibilityStatus)))

          await(TestGetEligibilityStatusService.getEligibilityStatus) mustBe eligibilityStatus
        }
      }
      "return the eligibility status from the API and save to session" when {
        "not available in session" in {
          enable(SignalControlGatewayEligibility)

          mockFetchEligibilityStatus(Right(None))
          mockGetNino(testNino)
          mockGetUTR(testUtr)
          mockGetEligibilityStatus(testNino, testUtr)(Right(eligibilityStatus))
          mockSaveEligibilityStatus(eligibilityStatus)(Right(SaveSessionDataSuccessResponse))

          await(TestGetEligibilityStatusService.getEligibilityStatus) mustBe eligibilityStatus
        }
      }
      "throw an exception" when {
        "there was a problem retrieving the eligibility status from session" in {
          enable(SignalControlGatewayEligibility)

          mockFetchEligibilityStatus(Left(GetSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus))
            .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure fetching eligibility status from session: UnexpectedStatusFailure(500)"
        }
        "there was a problem retrieving the eligibility status from the API" in {
          enable(SignalControlGatewayEligibility)

          val httpResponse = HttpResponse(BAD_REQUEST, "")

          mockFetchEligibilityStatus(Right(None))
          mockGetNino(testNino)
          mockGetUTR(testUtr)
          mockGetEligibilityStatus(testNino, testUtr)(Left(HttpConnectorError(httpResponse)))

          intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus))
            .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure fetching eligibility status from API: status = 400, body = "
        }
        "there was a problem saving the eligibility status to session" in {
          enable(SignalControlGatewayEligibility)

          mockFetchEligibilityStatus(Right(None))
          mockGetNino(testNino)
          mockGetUTR(testUtr)
          mockGetEligibilityStatus(testNino, testUtr)(Right(eligibilityStatus))
          mockSaveEligibilityStatus(eligibilityStatus)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus))
            .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure saving eligibility status to session: UnexpectedStatusFailure(500)"
        }
      }
    }

    "the signal control gateway eligibility feature switch is disabled" must {
      "return the eligibility status from session" when {
        "available in session" in {
          mockFetchEligibilityStatus(Right(Some(eligibilityStatus)))

          await(TestGetEligibilityStatusService.getEligibilityStatus) mustBe eligibilityStatus
        }
      }
      "return the eligibility status from the API and save to session" when {
        "not available in session" in {
          mockFetchEligibilityStatus(Right(None))
          mockGetUTR(testUtr)
          mockGetEligibilityStatus(testUtr)(Right(eligibilityStatus))
          mockSaveEligibilityStatus(eligibilityStatus)(Right(SaveSessionDataSuccessResponse))

          await(TestGetEligibilityStatusService.getEligibilityStatus) mustBe eligibilityStatus
        }
      }
      "throw an exception" when {
        "there was a problem retrieving the eligibility status from session" in {
          mockFetchEligibilityStatus(Left(GetSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus))
            .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure fetching eligibility status from session: UnexpectedStatusFailure(500)"
        }
        "there was a problem retrieving the eligibility status from the API" in {
          val httpResponse = HttpResponse(BAD_REQUEST, "")

          mockFetchEligibilityStatus(Right(None))
          mockGetUTR(testUtr)
          mockGetEligibilityStatus(testUtr)(Left(HttpConnectorError(httpResponse)))

          intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus))
            .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure fetching eligibility status from API: status = 400, body = "
        }
        "there was a problem saving the eligibility status to session" in {
          mockFetchEligibilityStatus(Right(None))
          mockGetEligibilityStatus(testUtr)(Right(eligibilityStatus))
          mockGetUTR(testUtr)
          mockSaveEligibilityStatus(eligibilityStatus)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus))
            .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure saving eligibility status to session: UnexpectedStatusFailure(500)"
        }
      }
    }
  }


}
