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

package connectors.individual.eligibility.httpparsers

import connectors.individual.eligibility.httpparsers.GetEligibilityStatusHttpParser.GetEligibilityStatusHttpReads
import utilities.HttpResult.HttpConnectorError
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class GetEligibilityStatusHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "GET"
  val testUri = "/"
  val eligibleKey = "eligible"

  "GetEligibilityStatusHttpReads" when {
    "read" should {
      "parse a correctly formatted OK Eligible response as a Boolean" in {
        val httpResponse = HttpResponse(OK, json = Json.obj(eligibleKey -> true), Map.empty)

        val res = GetEligibilityStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe Eligible
      }
      "parse a correctly formatted OK Ineligible response as a Boolean" in {
        val httpResponse = HttpResponse(OK, json = Json.obj(eligibleKey -> false), Map.empty)

        val res = GetEligibilityStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe Ineligible
      }

      "parse an incorrectly formatted OK response as a HttpConnectorError with JsError" in {
        val httpResponse = HttpResponse(OK, json = Json.obj(), Map.empty)

        val res = GetEligibilityStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe HttpConnectorError(httpResponse, _: Some[JsError])
      }

      "parse any other http status as a HttpResult[HttpConnectorError]" in {
        val httpResponse = HttpResponse(BAD_REQUEST, "")

        val res = GetEligibilityStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe HttpConnectorError(httpResponse)
      }
    }
  }
}
