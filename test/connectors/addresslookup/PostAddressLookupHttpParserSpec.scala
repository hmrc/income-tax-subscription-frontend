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

package connectors.addresslookup

import connectors.httpparser.addresslookup.PostAddressLookupHttpParser._
import org.scalatest.EitherValues
import play.api.libs.json.Json
import play.api.test.Helpers.{ACCEPTED, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class PostAddressLookupHttpParserSpec extends UnitTestTrait with EitherValues {

  val testHttpVerb = "POST"
  val testUri = "/"

  "PostAddressLookupHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response as a PostAddressLookupSuccessResponse" in {
        val httpResponse = HttpResponse(ACCEPTED, Some(Json.obj()), Map("Location" -> Seq("onRampUri")))

        val res = postAddressLookupHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Right(PostAddressLookupSuccessResponse(Some("onRampUri")))
      }
      "parse any other http status as a UnexpectedStatusFailure" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        val res = postAddressLookupHttpReads.read(testHttpVerb, testUri, httpResponse)

        res mustBe Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
