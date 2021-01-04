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

package connectors.subscriptiondata

import connectors.httpparser.GetSubscriptionDetailsHttpParser._
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class GetSubscriptionDetailsHttpParserSpec extends UnitTestTrait {


  val testHttpVerb = "GET"
  val testUri = "/"

  case class DummyModel(body: String)
  object DummyModel{
    implicit val format: OFormat[DummyModel] = Json.format[DummyModel]
  }

  "GetSelfEmploymentHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response and return the data in a model" in {
        val httpResponse = HttpResponse(OK, Some(Json.obj("body" -> "Test Body")))

        lazy val res = getSubscriptionDetailsHttpReads[DummyModel].read(testHttpVerb, testUri, httpResponse)

        res mustBe Some(DummyModel(body = "Test Body"))
      }
      "parse an incorrectly formatted Ok response as an invalid Json" in {
        val httpResponse = HttpResponse(OK, Some(Json.obj()))

        lazy val res = getSubscriptionDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        the[Exception] thrownBy res must have message "Invalid Json for getSubscriptionDetailsHttpReads"
      }

      "parse an no content response as None" in {
        val httpResponse = HttpResponse(NO_CONTENT)

        lazy val res = getSubscriptionDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)


        res mustBe None
      }

      "parse any other http status as a UnexpectedStatusFailure" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        lazy val res = getSubscriptionDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        the[Exception] thrownBy res must have message s"Unexpected status: $INTERNAL_SERVER_ERROR"

      }
    }

  }
}
