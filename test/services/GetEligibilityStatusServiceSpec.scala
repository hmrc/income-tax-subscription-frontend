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

import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import connectors.httpparser.{GetSessionDataHttpParser, SaveSessionDataHttpParser}
import models.EligibilityStatus
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.TestGetEligibilityStatusService
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}
import utilities.HttpResult.HttpConnectorError
import utilities.individual.TestConstants.testUtr

import scala.concurrent.Future

class GetEligibilityStatusServiceSpec extends TestGetEligibilityStatusService {

  val eligibilityStatus: EligibilityStatus = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)

  "getEligibilityStatus" must {
    "return the eligibility status from session" when {
      "available in session" in {
        mockFetchEligibilityStatus(Right(Some(eligibilityStatus)))

        await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr)) mustBe eligibilityStatus
      }
    }
    "return the eligibility status from the API and save to session" when {
      "not available in session" in {
        mockFetchEligibilityStatus(Right(None))
        mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibilityStatus)))
        mockSaveEligibilityStatus(eligibilityStatus)(Right(SaveSessionDataSuccessResponse))

        await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr)) mustBe eligibilityStatus
      }
    }
    "throw an exception" when {
      "there was a problem retrieving the eligibility status from session" in {
        mockFetchEligibilityStatus(Left(GetSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr)))
          .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure fetching eligibility status from session: UnexpectedStatusFailure(500)"
      }
      "there was a problem retrieving the eligibility status from the API" in {
        val httpResponse = HttpResponse(BAD_REQUEST, "")

        mockFetchEligibilityStatus(Right(None))
        mockGetEligibilityStatus(testUtr)(Future.successful(Left(HttpConnectorError(httpResponse))))

        intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr)))
          .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure fetching eligibility status from API: status = 400, body = "
      }
      "there was a problem saving the eligibility status to session" in {
        mockFetchEligibilityStatus(Right(None))
        mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibilityStatus)))
        mockSaveEligibilityStatus(eligibilityStatus)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr)))
          .message mustBe "[GetEligibilityStatusService][getEligibilityStatus] - failure saving eligibility status to session: UnexpectedStatusFailure(500)"
      }
    }
  }

}
