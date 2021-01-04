/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.individual.subscription.httpparsers

import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponseHttpReads
import models.common.subscription.{BadlyFormattedSubscriptionResponse, SubscriptionFailureResponse, SubscriptionSuccess}
import utilities.individual.TestConstants._
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class SubscriptionResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"
  val testUri = "/"

  "SubscriptionResponseHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response as a SubscriptionSuccess" in {
        val httpResponse = HttpResponse(OK, Json.toJson(SubscriptionSuccess(testMTDID)))

        val res = SubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe SubscriptionSuccess(testMTDID)
      }

      "parse an incorrectly formatted OK response as a BadlyFormattedSubscriptionResponse" in {
        val httpResponse = HttpResponse(OK, Json.obj())

        val res = SubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe BadlyFormattedSubscriptionResponse
      }

      "parse any other http status as a SubscriptionFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST)

        val res = SubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe SubscriptionFailureResponse(BAD_REQUEST)
      }
    }
  }
}
