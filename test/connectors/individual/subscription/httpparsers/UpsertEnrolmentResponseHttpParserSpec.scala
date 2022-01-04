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

package connectors.individual.subscription.httpparsers

import connectors.individual.subscription.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponseHttpReads
import models.common.subscription.{KnownFactsFailure, KnownFactsSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class UpsertEnrolmentResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "PUT"
  val testUri = "/"

  "UpsertEnrolmentResponseHttpReads" when {
    "read" should {
      "parse a NO_CONTENT response as an UpsertEnrolmentSuccess" in {
        val httpResponse = HttpResponse(NO_CONTENT)

        val res = UpsertEnrolmentResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe KnownFactsSuccess
      }

      "parse any other  response as an UpsertEnrolmentSuccess" in {
        val httpResponse = HttpResponse(BAD_REQUEST, Json.obj())

        val res = UpsertEnrolmentResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe KnownFactsFailure(httpResponse.body)
      }
    }
  }
}
