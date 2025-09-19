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

import models.ErrorModel
import models.prepop.{PrePopData, PrePopSelfEmployment}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HttpResponse

class GetPrePopDataHttpParserSpec extends PlaySpec {

  val testHttpVerb = "GET"
  val testUri = "/"

  val validJson: JsObject = Json.obj(
    "selfEmployment" -> Json.arr(
      Json.obj()
    )
  )

  "GetPrePopDataHttpParser" when {
    "read" should {
      "parse a correctly formatted OK response and return the prepop data" in {
        val httpResponse = HttpResponse(OK, json = validJson, headers = Map.empty)

        val res = GetPrePopDataParser.getPrePopDataResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Right(PrePopData(
          selfEmployment = Some(Seq(PrePopSelfEmployment(
            name = None,
            trade = None,
            address = None,
            startDate = None
          )))
        ))
      }
      "produce an error" when {
        "the json could not be parsed" in {
          val httpResponse = HttpResponse(OK, json = Json.arr(), headers = Map.empty)

          val res = GetPrePopDataParser.getPrePopDataResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

          res mustBe Left(ErrorModel(
            status = OK,
            reason = "Unable to parse json received into a PrePopData model"
          ))
        }
        "a non OK response was received" in {
          val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, "failure body")

          val res = GetPrePopDataParser.getPrePopDataResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

          res mustBe Left(ErrorModel(
            status = INTERNAL_SERVER_ERROR,
            reason = "failure body"
          ))
        }
      }
    }

  }
}
