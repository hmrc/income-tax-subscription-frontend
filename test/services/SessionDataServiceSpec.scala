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
import connectors.httpparser.{DeleteSessionDataHttpParser, GetSessionDataHttpParser, SaveSessionDataHttpParser}
import connectors.mocks.MockSessionDataConnector
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier

class SessionDataServiceSpec extends PlaySpec with MockSessionDataConnector {

  trait Setup {
    val service: SessionDataService = new SessionDataService(mockSessionDataConnector)
  }

  val testReference: String = "test-reference"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "fetchReference" must {
    "return a reference" when {
      "the connector returns a valid result" in new Setup {
        mockGetSessionData(ITSASessionKeys.REFERENCE)(Right(Some(testReference)))

        await(service.fetchReference) mustBe Right(Some(testReference))
      }
    }
    "return no reference" when {
      "the connector returns no data" in new Setup {
        mockGetSessionData(ITSASessionKeys.REFERENCE)(Right(None))

        await(service.fetchReference) mustBe Right(None)
      }
    }
    "return an error" when {
      "the connector returns an error" in new Setup {
        mockGetSessionData(ITSASessionKeys.REFERENCE)(Left(GetSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        await(service.fetchReference) mustBe Left(GetSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "saveReference" must {
    "return a success response" when {
      "the connector returns a success response" in new Setup {
        mockSaveSessionData(ITSASessionKeys.REFERENCE, testReference)(Right(SaveSessionDataHttpParser.SaveSessionDataSuccessResponse))

        await(service.saveReference(testReference)) mustBe Right(SaveSessionDataHttpParser.SaveSessionDataSuccessResponse)
      }
    }
    "return a failure response" when {
      "the connector returns a failure response" in new Setup {
        mockSaveSessionData(ITSASessionKeys.REFERENCE, testReference)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        await(service.saveReference(testReference)) mustBe Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "deleteReference" must {
    "return a success response" when {
      "the connector returns a success response" in new Setup {
        mockDeleteSessionData(ITSASessionKeys.REFERENCE)(Right(DeleteSessionDataHttpParser.DeleteSessionDataSuccessResponse))

        await(service.deleteReference) mustBe Right(DeleteSessionDataHttpParser.DeleteSessionDataSuccessResponse)
      }
    }
    "return a failure response" when {
      "the connector returns a failure response" in new Setup {
        mockDeleteSessionData(ITSASessionKeys.REFERENCE)(Left(DeleteSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        await(service.deleteReference) mustBe Left(DeleteSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
