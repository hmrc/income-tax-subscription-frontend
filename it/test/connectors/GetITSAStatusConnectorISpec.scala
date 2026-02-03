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
import helpers.servicemocks.GetITSAStatusStub.stubGetITSAStatus
import models.ErrorModel
import models.status.GetITSAStatus.NoStatus
import models.status.MandationStatus.Voluntary
import models.status.{GetITSAStatusModel, GetITSAStatusRequest}
import org.scalatest.matchers.must.Matchers.*
import org.scalatest.matchers.should.Matchers.*
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

class GetITSAStatusConnectorISpec extends ComponentSpecBase {

  private lazy val connector: GetITSAStatusConnector = app.injector.instanceOf[GetITSAStatusConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val testNino: String = "test-nino"

  "GetITSAStatusConnector" must {
    "return a GetITSAStatusModel" when {
      "an OK response was received with valid json" in {
        stubGetITSAStatus(
          Json.toJson(GetITSAStatusRequest(testNino))
        )(OK, Json.toJson(GetITSAStatusModel(status = NoStatus)))

        val result = connector.getITSAStatus(testNino)

        result.futureValue shouldBe Right(GetITSAStatusModel(status = NoStatus))
      }
    }
  }

}
