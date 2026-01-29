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
import models.status.GetITSAStatus.NoStatus
import models.status.GetITSAStatusModel
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.test.Helpers.*
import services.mocks.{MockGetITSAStatusConnector, MockNinoService, MockSessionDataService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global

class GetITSAStatusServiceSpec extends PlaySpec
  with Matchers
  with MockGetITSAStatusConnector
  with MockSessionDataService
  with MockNinoService {

  val testNino: String = "test-nino"
  val getITSAStatus: GetITSAStatusModel = GetITSAStatusModel(status = NoStatus)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val service: GetITSAStatusService = new GetITSAStatusService(
      mockGetITSAStatusConnector,
      mockNinoService,
      mockSessionDataService
    )
  }

  "getITSAStatus" must {
    "return the itsa status from session" when {
      "available in session" in new Setup {
        val sessionData = SessionData(Map(
          ITSASessionKeys.GET_ITSA_STATUS -> Json.toJson(getITSAStatus)
        ))

        await(service.getITSAStatus(sessionData)) mustBe getITSAStatus
      }
    }
    "return the itsa status from the API and save to the session database" when {
      "the itsa status is not available from session" in new Setup {
        mockGetNino(testNino)
        mockGetITSAStatusSuccess(testNino)(NoStatus)
        mockSaveGetITSAStatus(getITSAStatus)(Right(SaveSessionDataSuccessResponse))

        await(service.getITSAStatus(SessionData())) mustBe getITSAStatus
      }
    }
    "throw an exception" when {
      "there was a problem retrieving get itsa status from the API" in new Setup {
        mockGetNino(testNino)
        mockGetITSAStatusFailure(testNino)

        intercept[InternalServerException](await(service.getITSAStatus(SessionData())))
          .message mustBe "[GetITSAStatusService][getITSAStatus] - Failure when fetching get itsa status from API: ErrorModel(500,Something went wrong)"
      }
      "there was a problem saving get itsa status to session" in new Setup {
        mockGetNino(testNino)
        mockGetITSAStatusSuccess(testNino)(NoStatus)
        mockSaveGetITSAStatus(getITSAStatus)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(service.getITSAStatus(SessionData())))
          .message mustBe "[GetITSAStatusService][getITSAStatus] - Failure when saving get itsa status to session: UnexpectedStatusFailure(500)"
      }
    }
  }
}
