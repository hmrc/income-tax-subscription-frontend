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

package incometax.subscription.httpparsers

import incometax.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponseHttpReads
import incometax.subscription.models.{SubscriptionFailureResponse, SubscriptionSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import core.utils.TestConstants._
import core.utils.UnitTestTrait

class GetSubscriptionResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "GET"
  val testUri = "/"

  "GetSubscriptionResponseHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response as a Some(SubscriptionSuccess)" in {
        val httpResponse = HttpResponse(OK, Json.toJson(SubscriptionSuccess(testMTDID)))

        val res = GetSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe Some(SubscriptionSuccess(testMTDID))
      }

      "parse an empty OK response as a None" in {
        val httpResponse = HttpResponse(OK, Json.obj())

        val res = GetSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe empty
      }

      "parse any other http status as a SubscriptionFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST)

        val res = GetSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe SubscriptionFailureResponse(BAD_REQUEST)
      }
    }
  }
}
