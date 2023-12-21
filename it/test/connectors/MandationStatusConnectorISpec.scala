/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import helpers.ComponentSpecBase
import helpers.servicemocks.MandationStatusStub.{stubGetMandationStatus, stubGetMandationStatusInvalidResponse}
import models.ErrorModel
import models.status.MandationStatus.Voluntary
import models.status.{MandationStatusModel, MandationStatusRequest}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

class MandationStatusConnectorISpec extends ComponentSpecBase {
  private lazy val connector: MandationStatusConnector = app.injector.instanceOf[MandationStatusConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "MandationStatusConnector" must {
    "return MandationStatusResponse" when {
      "the status determination service returns OK and a valid JSON response body" in {
        stubGetMandationStatus(
          Json.toJson(MandationStatusRequest("test-nino", "test-utr"))
        )(OK, Json.toJson(MandationStatusModel(currentYearStatus = Voluntary, nextYearStatus = Voluntary)))

        val result = connector.getMandationStatus("test-nino", "test-utr")

        result.futureValue shouldBe Right(MandationStatusModel(currentYearStatus = Voluntary, nextYearStatus = Voluntary))
      }
    }

    "return an exception" when {
      "the status determination service returns OK and an invalid JSON response body" in {
        stubGetMandationStatusInvalidResponse(
          Json.toJson(MandationStatusRequest("test-nino", "test-utr"))
        )(OK, "{ currentYearStatus")

        val result = connector.getMandationStatus("test-nino", "test-utr")

        result.futureValue shouldBe Left(ErrorModel(OK, "Invalid Json for mandationStatusResponseHttpReads: List((,List(JsonValidationError(List(error.expected.jsobject),List()))))"))
      }
    }

    "return the status and error received" when {
      "the status determination service returns a failure" in {
        stubGetMandationStatus(
          Json.toJson(MandationStatusRequest("test-nino", "test-utr"))
        )(INTERNAL_SERVER_ERROR, failureResponse("code", "reason"))

        val result = connector.getMandationStatus("test-nino", "test-utr")

        result.futureValue shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, """{"code":"code","reason":"reason"}"""))
      }
    }
  }

  private def failureResponse(code: String, reason: String): JsValue =
    Json.parse(s"""
       |{
       |  "code":"$code",
       |  "reason":"$reason"
       |}
    """.stripMargin)
}
