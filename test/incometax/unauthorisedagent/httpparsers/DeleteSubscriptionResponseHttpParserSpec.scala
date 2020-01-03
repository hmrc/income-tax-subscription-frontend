/*
 * Copyright 2020 HM Revenue & Customs
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

import core.utils.UnitTestTrait
import incometax.unauthorisedagent.httpparsers.DeleteSubscriptionResponseHttpParser.DeleteSubscriptionResponseHttpReads
import incometax.unauthorisedagent.models.{DeleteSubscriptionFailure, DeleteSubscriptionSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class DeleteSubscriptionResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "PUT"
  val testUri = "/"

  "DeleteSubscriptionResponseHttpReads" when {
    "read" should {
      "parse a CREATED response as an DeleteSubscriptionSuccess" in {
        val httpResponse = HttpResponse(NO_CONTENT)

        val res = DeleteSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe DeleteSubscriptionSuccess
      }

      "parse any other  response as an DeleteSubscriptionFailure" in {
        val httpResponse = HttpResponse(BAD_REQUEST, Json.obj())

        val res = DeleteSubscriptionResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe DeleteSubscriptionFailure(httpResponse.body)
      }
    }
  }
}
