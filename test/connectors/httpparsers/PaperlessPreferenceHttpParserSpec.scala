/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors.httpparsers

import connectors.httpparsers.PaperlessPreferenceHttpParser.PaperlessPreferenceHttpReads
import connectors.httpparsers.SubscriptionResponseHttpParser._
import connectors.models.preferences._
import connectors.models.subscription._
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.play.http.HttpResponse
import utils.HttpResult.HttpConnectorError
import utils.TestConstants._
import utils.UnitTestTrait

class PaperlessPreferenceHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"
  val testUri = "/"

  "PaperlessPreferenceHttpReads" when {
    "read" should {
      "parse a correctly formatted OK true response as Activated" in {
        val httpResponse = HttpResponse(OK, Some(Json.obj("optedIn" -> true)))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe Activated
      }

      "parse a correctly formatted OK false response as Declined" in {
        val httpResponse = HttpResponse(OK, Some(Json.obj("optedIn" -> false)))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe Declined
      }

      "parse a correctly formatted PRECONDITION_FAILED false response as Declined" in {
        val httpResponse = HttpResponse(PRECONDITION_FAILED)

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe Unset
      }

      "parse an incorrectly formatted OK response as a PaperlessPreferenceError" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(OK, Some(json))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe HttpConnectorError(httpResponse, Some(json.validate[PaperlessState].asInstanceOf[JsError]))
      }

      "parse any other http status as a SubscriptionFailureResponse" in {
        val httpResponse = HttpResponse(responseStatus = BAD_REQUEST, responseString = Some(testErrorMessage))

        val res = PaperlessPreferenceHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe HttpConnectorError(httpResponse)
      }
    }
  }
}
