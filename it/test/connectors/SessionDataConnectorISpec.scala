/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.httpparser.GetSessionDataHttpParser.{InvalidJson, UnexpectedStatusFailure}
import connectors.stubs.SessionDataConnectorStub.stubGetSessionData
import helpers.ComponentSpecBase
import play.api.libs.json.{JsObject, Json, OFormat}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class SessionDataConnectorISpec extends ComponentSpecBase {

  lazy val connector: SessionDataConnector = app.injector.instanceOf[SessionDataConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val id: String = "test-id"

  case class DummyModel(body: String)

  object DummyModel {
    implicit val format: OFormat[DummyModel] = Json.format[DummyModel]
  }

  "getSessionData" should {
    "return the provided model" in {
      val successfulResponseBody: JsObject = Json.obj("body" -> "Test Body")

      stubGetSessionData(id)(OK, successfulResponseBody)

      val res = connector.getSessionData[DummyModel](id)

      await(res) mustBe Right(Some(DummyModel(body = "Test Body")))
    }

    "Return InvalidJson" in {
      stubGetSessionData(id)(OK, Json.obj())

      val res = connector.getSessionData[DummyModel](id)

      await(res) mustBe Left(InvalidJson)
    }

    "Return None" in {
      stubGetSessionData(id)(NO_CONTENT, Json.obj())

      val res = connector.getSessionData[DummyModel](id)

      await(res) mustBe Right(None)

    }
    "Return UnexpectedStatusFailure" in {
      stubGetSessionData(id)(INTERNAL_SERVER_ERROR, Json.obj())

      val res = connector.getSessionData[DummyModel](id)

      await(res) mustBe Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))

    }
  }

}
