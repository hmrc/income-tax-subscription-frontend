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

package connectors.subscriptiondata

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser._
import org.scalatest.EitherValues
import play.api.libs.json.Json
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait


class DeleteSubscriptionDetailsHttpParserSpec extends UnitTestTrait with EitherValues {

  val testHttpVerb = "DELETE"
  val testUri = "/"

  "DeleteSubscriptionDetailsHttpReads" when {
    "read" should {
      "parse a OK response as a PostSelfEmploymentsSuccessResponse" in {
        val httpResponse = HttpResponse(OK, json = Json.obj(), Map.empty)

        val res = deleteSubscriptionDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Right(DeleteSubscriptionDetailsSuccessResponse)
      }

      "parse any other http status as a UnexpectedStatusFailure" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, body = "")

        val res = deleteSubscriptionDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
