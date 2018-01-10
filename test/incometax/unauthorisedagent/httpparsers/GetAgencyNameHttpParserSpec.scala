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

package incometax.unauthorisedagent.httpparsers

import core.utils.TestConstants._
import core.utils.UnitTestTrait
import incometax.unauthorisedagent.httpparsers.RetrieveSubscriptionResponseHttpParser.RetrieveSubscriptionResponseHttpReads
import incometax.unauthorisedagent.models.{GetAgencyNameFailure, GetAgencyNameSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import GetAgencyNameResponseHttpParser._

class GetAgencyNameHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "GET"
  val testUri = "/"

  "StoreSubscriptionResponseHttpReads" when {
    "read" should {
      "parse an OK response as a GetAgencyNameSuccess" in {
        val response = GetAgencyNameSuccess(testAgencyName)
        val httpResponse = HttpResponse(OK, Json.toJson(response))

        val res = getAgencyNameResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe response
      }

      "parse any other response as a GetAgencyNameFailure" in {
        val httpResponse = HttpResponse(BAD_REQUEST, Json.obj())

        val res = getAgencyNameResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe GetAgencyNameFailure(httpResponse.body)
      }
    }
  }
}
