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

import connectors.httpparser.{DeleteSessionDataHttpParser, SaveSessionDataHttpParser}
import connectors.stubs.SessionDataConnectorStub.{stubDeleteSessionData, stubGetAllSessionData, stubSaveSessionData}
import helpers.ComponentSpecBase
import models.SessionData
import play.api.libs.json.{JsValue, Json, OFormat}
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

  val dummyModel: DummyModel = DummyModel(body = "Test Body")

  val dummyModelJson: JsValue = Json.obj(
    "body" -> "Test Body"
  )

  "getAllSessionData" should {
    "return the provided model" in {
      val sessionData = Map(
        id -> dummyModelJson
      )

      stubGetAllSessionData(sessionData, false)

      val res = await(connector.getAllSessionData())

      res mustBe Right(Some(sessionData))
    }
    "Return no data" in {
      stubGetAllSessionData(Map(), false)

      val res = await(connector.getAllSessionData())

      res mustBe Right(None)
    }
  }

  "saveSessionData" should {
    "return a success response" in {
      stubSaveSessionData(id, dummyModel)(OK)

      val res = connector.saveSessionData(id, dummyModel)

      await(res) mustBe Right(SaveSessionDataHttpParser.SaveSessionDataSuccessResponse)
    }
    "return an UnexpectedStatusFailure" in {
      stubSaveSessionData(id, dummyModel)(INTERNAL_SERVER_ERROR)

      val res = connector.saveSessionData(id, dummyModel)

      await(res) mustBe Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
    }
  }

  "deleteSessionData" should {
    "return a success response" in {
      stubDeleteSessionData(id)(OK)

      val res = connector.deleteSessionData(id)

      await(res) mustBe Right(DeleteSessionDataHttpParser.DeleteSessionDataSuccessResponse)
    }
    "return an UnexpectedStatusFailure" in {
      stubDeleteSessionData(id)(INTERNAL_SERVER_ERROR)

      val res = connector.deleteSessionData(id)

      await(res) mustBe Left(DeleteSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
    }
  }
}
