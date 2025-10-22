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
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.JsString
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.individual.mocks.MockAuthService
import services.mocks.MockSessionDataService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global

class NinoServiceSpec extends PlaySpec with Matchers with MockAuthService with MockSessionDataService {

  val testNino: String = "test-nino"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val service: NinoService = new NinoService(
      mockAuthService,
      mockSessionDataService
    )
  }

  "getNino" must {
    "return a nino" when {
      "the nino was returned from session" in new Setup {
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.NINO -> JsString(testNino)
        )))

        await(service.getNino(SessionData())) mustBe testNino
      }
      "the nino was returned from the users auth profile" in new Setup {
        mockGetAllSessionData(SessionData())
        mockRetrievalSuccess[Option[String]](Some(testNino))
        mockSaveNino(testNino)(Right(SaveSessionDataSuccessResponse))

        await(service.getNino(SessionData())) mustBe testNino
      }
    }
    "throw an exception" when {
      "no nino was returned from auth" in new Setup {
        mockGetAllSessionData(SessionData())
        mockRetrievalSuccess[Option[String]](None)

        intercept[InternalServerException](await(service.getNino(SessionData())))
          .message mustBe "[NinoService][getNino] - Nino not present in auth"
      }
      "there was a problem saving the nino to session" in new Setup {
        mockGetAllSessionData(SessionData())
        mockRetrievalSuccess[Option[String]](Some(testNino))
        mockSaveNino(testNino)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(service.getNino(SessionData())))
          .message mustBe "[NinoService][getNino] - Failure when saving nino to session: UnexpectedStatusFailure(500)"
      }
    }
  }
}
