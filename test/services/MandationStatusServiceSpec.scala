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
import connectors.httpparser.SaveSessionDataHttpParser
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import models.SessionData
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.{MockMandationStatusConnector, MockNinoService, MockSessionDataService, MockUTRService}
import uk.gov.hmrc.http.InternalServerException

class MandationStatusServiceSpec extends PlaySpec
  with Matchers
  with MockMandationStatusConnector
  with MockSessionDataService
  with MockNinoService
  with MockUTRService {

  val testNino: String = "test-nino"
  val testUtr: String = "test-utr"

  val mandationStatusModel: MandationStatusModel = MandationStatusModel(Voluntary, Voluntary)

  trait Setup {
    val service: MandationStatusService = new MandationStatusService(
      mockMandationStatusConnector,
      mockNinoService,
      mockUTRService,
      mockSessionDataService
    )
  }

  "getMandationStatus" must {
    "return the mandation status from session" when {
      "available in session" in new Setup {
        val sessionData = SessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(mandationStatusModel)
        ))

        await(service.getMandationStatus(sessionData)) mustBe mandationStatusModel
      }
    }
    "return the mandation status from the API and save to the session database" when {
      "the mandation status is not available from session" in new Setup {
        mockGetNino(testNino)
        mockGetUTR(testUtr)
        mockGetMandationStatus(testNino, testUtr)(Voluntary, Voluntary)
        mockSaveMandationStatus(mandationStatusModel)(Right(SaveSessionDataSuccessResponse))

        await(service.getMandationStatus()) mustBe mandationStatusModel
      }
    }
    "throw an exception" when {
      "there was a problem retrieving mandation status from the API" in new Setup {
        mockGetAllSessionData(SessionData())
        mockGetNino(testNino)
        mockGetUTR(testUtr)
        mockFailedGetMandationStatus()

        intercept[InternalServerException](await(service.getMandationStatus()))
          .message mustBe "[MandationStatusService][getMandationStatus] - Failure when fetching mandation status from API: ErrorModel(500,Something went wrong)"
      }
      "there was a problem saving mandation status to session" in new Setup {
        mockGetAllSessionData(SessionData())
        mockGetNino(testNino)
        mockGetUTR(testUtr)
        mockGetMandationStatus(testNino, testUtr)(Voluntary, Voluntary)
        mockSaveMandationStatus(mandationStatusModel)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(service.getMandationStatus()))
          .message mustBe "[MandationStatusService][getMandationStatus] - Failure when saving mandation status to session: UnexpectedStatusFailure(500)"
      }
    }
  }
}
