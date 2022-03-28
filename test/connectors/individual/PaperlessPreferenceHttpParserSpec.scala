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

package connectors.individual

import connectors.PaperlessPreferenceHttpParser.PaperlessPreferenceHttpReads
import models.{Activated, Unset}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utilities.HttpResult.HttpConnectorError
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

class PaperlessPreferenceHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"

  "PaperlessPreferenceHttpReads" when {
    "read" should {
      "parse a correctly formatted OK true response as Activated" in {
        val httpResponse = HttpResponse(OK, json = Json.obj("optedIn" -> true), Map.empty)

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.value mustBe Activated
      }

      s"parse a correctly formatted OK false response with a redirect url as Declined($testUrl)" in {
        val httpResponse = HttpResponse(OK,
          json = Json.obj(
            "optedIn" -> false,
            "redirectUserTo" -> testUrl
          ),
          Map.empty
        )

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.value mustBe Unset(testUrl)
      }

      "parse a correctly formatted PRECONDITION_FAILED false response as Unset" in {
        val httpResponse = HttpResponse(PRECONDITION_FAILED, json = Json.obj("redirectUserTo" -> testUrl), Map.empty)

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.value mustBe Unset(testUrl)
      }

      "parse an incorrectly formatted OK response as a PaperlessPreferenceError" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(OK, json = json, Map.empty)

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.left.value.httpResponse mustBe httpResponse
      }


      "parse an incorrectly formatted PRECONDITION_FAILED response as a PaperlessPreferenceError" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(PRECONDITION_FAILED, json = json, Map.empty)

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.left.value.httpResponse mustBe httpResponse
      }

      "parse any other http status as a SubscriptionFailureResponse" in {
        val httpResponse = HttpResponse(status = BAD_REQUEST, body = testErrorMessage)

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.left.value mustBe HttpConnectorError(httpResponse)
      }
    }
  }
}
