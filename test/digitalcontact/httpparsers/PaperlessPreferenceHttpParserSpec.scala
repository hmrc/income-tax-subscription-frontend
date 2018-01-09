/*
 * Copyright 2018 HM Revenue & Customs
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

package digitalcontact.httpparsers

import digitalcontact.httpparsers.PaperlessPreferenceHttpParser.PaperlessPreferenceHttpReads
import digitalcontact.models.{Activated, Unset}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import core.utils.HttpResult.HttpConnectorError
import core.utils.TestConstants._
import core.utils.UnitTestTrait

class PaperlessPreferenceHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"

  "PaperlessPreferenceHttpReads" when {
    "read" should {
      "parse a correctly formatted OK true response as Activated" in {
        val httpResponse = HttpResponse(OK, Some(Json.obj("optedIn" -> true)))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.right.value mustBe Activated
      }

      "parse a correctly formatted OK false response without a redirect url as Declined(None)" in {
        val httpResponse = HttpResponse(OK, Some(Json.obj("optedIn" -> false)))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.right.value mustBe Unset(None)
      }

      s"parse a correctly formatted OK false response with a redirect url as Declined($testUrl)" in {
        val httpResponse = HttpResponse(OK, Some(
          Json.obj(
            "optedIn" -> false,
            "redirectUserTo" -> testUrl
          )
        ))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.right.value mustBe Unset(testUrl)
      }

      "parse a correctly formatted PRECONDITION_FAILED false response as Unset" in {
        val httpResponse = HttpResponse(PRECONDITION_FAILED, Some(Json.obj("redirectUserTo" -> testUrl)))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.right.value mustBe Unset(testUrl)
      }

      "parse an incorrectly formatted OK response as a PaperlessPreferenceError" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(OK, Some(json))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.left.value.httpResponse mustBe httpResponse
      }


      "parse an incorrectly formatted PRECONDITION_FAILED response as a PaperlessPreferenceError" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(PRECONDITION_FAILED, Some(json))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.left.value.httpResponse mustBe httpResponse
      }

      "parse any other http status as a SubscriptionFailureResponse" in {
        val httpResponse = HttpResponse(responseStatus = BAD_REQUEST, responseString = Some(testErrorMessage))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUrl, httpResponse)

        res.left.value mustBe HttpConnectorError(httpResponse)
      }
    }
  }
}
