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

package connectors.subscriptiondata

import connectors.httpparser.RetrieveReferenceHttpParser._
import org.scalatest.EitherValues
import play.api.libs.json.Json
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class RetrieveReferenceHttpParserSpec extends UnitTestTrait with EitherValues {

  val testHttpVerb = "POST"
  val testUri = "/"

  "RetrieveReferenceHttpParser" when {
    "read" should {
      "parse a correctly formatted OK response as a PostSelfEmploymentsSuccessResponse" in {
        val httpResponse = HttpResponse(
          status = OK,
          json = Json.obj(
            "reference" -> "test-reference"
          ),
          headers = Map.empty
        )

        val res = retrieveReferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Right("test-reference")
      }

      "parse a incorrectly formatted OK response as a InvalidJsonFailure" in {
        val httpResponse = HttpResponse(
          status = OK,
          json = Json.obj(),
          headers = Map.empty
        )

        val res = retrieveReferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Left(InvalidJsonFailure)
      }

      "parse any other http status as a UnexpectedStatusFailure" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        val res = retrieveReferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
