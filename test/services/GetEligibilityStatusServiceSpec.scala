/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.individual.eligibility.httpparsers.{Eligible, Ineligible}
import play.api.libs.json.JsError
import play.api.test.Helpers._
import services.mocks.TestGetEligibilityStatusService
import uk.gov.hmrc.http.HttpResponse
import utilities.HttpResult.HttpConnectorError
import utilities.individual.TestConstants.testUtr

import scala.concurrent.Future

class GetEligibilityStatusServiceSpec extends TestGetEligibilityStatusService {

  "getEligibilityStatus" should {
    "return Eligible" when {
      "the GetEligibilityStatusConnector returns OK and true" in {
        mockGetEligibilityStatus(testUtr)(Future.successful(Right(Eligible)))

        val res = await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr))

        res mustBe Right(Eligible)
      }
    }
    "return Ineligible" when {
      "the GetEligibilityStatusConnector returns OK and false" in {
        mockGetEligibilityStatus(testUtr)(Future.successful(Right(Ineligible)))

        val res = await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr))

        res mustBe Right(Ineligible)
      }
    }
    "return InvalidJsonError" when {
      "the GetEligibilityStatusConnector returns OK and cannot parse the received json" in {
        val httpResponse = HttpResponse(OK)
        mockGetEligibilityStatus(testUtr)(Future.successful(Left(HttpConnectorError(httpResponse, Some(JsError("Invalid Json"))))))

        val res = await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr))

        res mustBe Left(HttpConnectorError(httpResponse, JsError("Invalid Json")))
      }
    }
    "return GetEligibilityStatusConnectorFailure" when {
      "the GetEligibilityStatusConnector returns any other HTTP status code" in {
        val httpResponse = HttpResponse(BAD_REQUEST)
        mockGetEligibilityStatus(testUtr)(Future.successful(Left(HttpConnectorError(httpResponse))))

        val res = await(TestGetEligibilityStatusService.getEligibilityStatus(testUtr))

        res mustBe Left(HttpConnectorError(httpResponse))
      }
    }
  }

}
