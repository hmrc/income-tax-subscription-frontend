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

import core.utils.TestModels._
import core.utils.UnitTestTrait
import incometax.unauthorisedagent.httpparsers.RetrieveSubscriptionResponseHttpParser.RetrieveSubscriptionResponseHttpReads
import incometax.unauthorisedagent.models.RetrieveSubscriptionFailure
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class RetrieveSubscriptionResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "PUT"
  val testUri = "/"

  "StoreSubscriptionResponseHttpReads" when {
    "read" should {
      "parse an OK response as a storedSubscription" in {
        val response = testStoredSubscription
        val httpResponse = HttpResponse(OK, Json.toJson(response))

        val res = RetrieveSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value must contain(testStoredSubscription)
      }

      "parse a NOT_FOUND response as a None" in {
        val httpResponse = HttpResponse(NOT_FOUND)

        val res = RetrieveSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe empty
      }

      "parse any other response as an RetrieveSubscriptionFailure" in {
        val httpResponse = HttpResponse(BAD_REQUEST, Json.obj())

        val res = RetrieveSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe RetrieveSubscriptionFailure(httpResponse.body)
      }
    }
  }
}
