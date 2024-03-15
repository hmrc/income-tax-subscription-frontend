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

package connectors.httpparser

import connectors.httpparser.GetSessionDataHttpParser._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json, OFormat}
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.HttpResponse

class GetSessionDataHttpParserSpec extends PlaySpec {

  val testHttpVerb = "GET"
  val testUri = "/"

  case class DummyModel(body: String)

  object DummyModel {
    implicit val format: OFormat[DummyModel] = Json.format[DummyModel]
  }

  "GetSessionDataHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response and return the data in a model" in {
        val httpResponse = HttpResponse(OK, json = Json.obj("body" -> "Test Body"), headers = Map.empty)
        val res = getSessionDataHttpReads[DummyModel].read(testHttpVerb, testUri, httpResponse)

        res mustBe Right(Some(DummyModel(body = "Test Body")))
      }
      "parse a correctly formatted OK response and return the data as a basic type" in {
        val httpResponse = HttpResponse(OK, json = JsString("test string"), headers = Map.empty)
        val res = getSessionDataHttpReads[String].read(testHttpVerb, testUri, httpResponse)

        res mustBe Right(Some("test string"))
      }
      "parse an incorrectly formatted Ok response as an invalid Json" in {
        val httpResponse = HttpResponse(OK, json = Json.obj(), headers = Map.empty)
        val res = getSessionDataHttpReads[DummyModel].read(testHttpVerb, testUri, httpResponse)

        res mustBe Left(InvalidJson)
      }
      "parse an no content response as None" in {
        val httpResponse = HttpResponse(NO_CONTENT, body = "")
        val res = getSessionDataHttpReads[DummyModel].read(testHttpVerb, testUri, httpResponse)

        res mustBe Right(None)
      }

      "parse any other http status as a UnexpectedStatusFailure" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, body = "")
        val res = getSessionDataHttpReads[DummyModel].read(testHttpVerb, testUri, httpResponse)

        res mustBe Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }

  }
}
