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

package incometax.subscription.httpparsers

import core.utils.UnitTestTrait
import incometax.subscription.httpparsers.StoreSubscriptionResponseHttpParser.StoreSubscriptionResponseHttpReads
import incometax.subscription.models.{StoreSubscriptionFailure, StoreSubscriptionSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class StoreSubscriptionResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "PUT"
  val testUri = "/"

  "StoreSubscriptionResponseHttpReads" when {
    "read" should {
      "parse a CREATED response as an StoreSubscriptionSuccess" in {
        val httpResponse = HttpResponse(CREATED)

        val res = StoreSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe StoreSubscriptionSuccess
      }

      "parse any other  response as an StoreSubscriptionSuccess" in {
        val httpResponse = HttpResponse(BAD_REQUEST, Json.obj())

        val res = StoreSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe StoreSubscriptionFailure(httpResponse.body)
      }
    }
  }
}
