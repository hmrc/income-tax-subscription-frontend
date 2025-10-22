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
import connectors.httpparser.DeleteSessionDataHttpParser.DeleteSessionDataSuccessResponse
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import connectors.httpparser.{DeleteSessionDataHttpParser, SaveSessionDataHttpParser}
import models.SessionData
import models.Yes.YES
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.{JsBoolean, JsString}
import play.api.mvc.{AnyContent, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, await, defaultAwaitTimeout}
import services.mocks.MockSessionDataService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global

class SessionClearingServiceSpec extends PlaySpec with MockSessionDataService {

  trait Setup {
    val service: SessionClearingService = new SessionClearingService(mockSessionDataService)
  }

  val testCall: Call = controllers.agent.matching.routes.ClientDetailsController.show()

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContent] = FakeRequest()

  "clearAgentSession" must {
    "redirect to correct url" when {
      "clearing session with email passed" in new Setup {
        val sessionData = SessionData(Map(
          ITSASessionKeys.EMAIL_PASSED -> JsBoolean(true)
        ))
        mockDeleteSessionAll(Right(DeleteSessionDataSuccessResponse))
        mockSaveEmailPassed(true)(Right(SaveSessionDataSuccessResponse))

        val res = await(service.clearAgentSession(testCall, sessionData))
        res.header.status mustBe SEE_OTHER
      }
      "clearing session with email not passed but consent passed" in new Setup {
        val sessionData = SessionData(Map(
          ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
        ))
        mockDeleteSessionAll(Right(DeleteSessionDataSuccessResponse))
        mockSaveEmailPassed(true)(Right(SaveSessionDataSuccessResponse))

        val res = await(service.clearAgentSession(testCall, sessionData))
        res.header.status mustBe SEE_OTHER
      }
    }
    "return an error" when {
      "the deletion of session data returns an error" in new Setup {
        mockDeleteSessionAll(Left(DeleteSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(service.clearAgentSession(testCall)))
          .message mustBe s"[SessionClearingService][deleteSessionData] - Unexpected failure: UnexpectedStatusFailure(500)"
      }
      "the saving of session data returns an error" in new Setup {
        val sessionData = SessionData(Map(
          ITSASessionKeys.EMAIL_PASSED -> JsBoolean(true)
        ))
        mockDeleteSessionAll(Right(DeleteSessionDataSuccessResponse))
        mockSaveEmailPassed(true)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(service.clearAgentSession(testCall, sessionData)))
          .message mustBe s"[SessionClearingService][saveEmailConsentCaptured] - Unexpected failure: UnexpectedStatusFailure(500)"
      }
    }
  }
}
