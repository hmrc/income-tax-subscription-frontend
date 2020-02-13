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

package connectors.usermatching.httpparsers

import java.time.OffsetDateTime

import connectors.usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusHttpReads
import core.utils.TestConstants._
import core.utils.UnitTestTrait
import models.usermatching.{LockedOut, LockoutStatusFailureResponse, NotLockedOut}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse

class LockoutStatusHttpParserSpec extends UnitTestTrait with EitherValues {
  val testUri = "/"

  val testTime = OffsetDateTime.now()
  val testResponsJson: JsValue = Json.parse(
    s"""
       | {
       |    "arn" : "$testNino",
       |    "expiryTimestamp" : "${testTime.toString}"
       | }
    """.stripMargin
  )

  "LockoutStatusHttpReads" when {

    "read for GET" should {
      val testHttpVerb = "GET"

      "parse a correctly formatted NOT_FOUND response for lockout status" in {
        val httpResponse = HttpResponse(NOT_FOUND)

        val res = LockoutStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe NotLockedOut
      }

      "parse a correctly formatted OK response for lockout status" in {
        val httpResponse = HttpResponse(OK, Some(testResponsJson))

        val res = LockoutStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe LockedOut(testNino, testTime)
      }

      "parse any other http status as a LockoutStatusFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST)

        val res = LockoutStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe LockoutStatusFailureResponse(BAD_REQUEST)
      }
    }

    "read for POST" should {
      val testHttpVerb = "POST"

      "parse a correctly formatted CREATED response for lockout status" in {
        val httpResponse = HttpResponse(CREATED, testResponsJson)

        val res = LockoutStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe LockedOut(testNino, testTime)
      }

      "parse any other http status as a LockoutStatusFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST)

        val res = LockoutStatusHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe LockoutStatusFailureResponse(BAD_REQUEST)
      }
    }
  }


}
