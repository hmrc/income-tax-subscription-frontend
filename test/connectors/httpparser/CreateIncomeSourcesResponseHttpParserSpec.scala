/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.httpparser

import connectors.httpparser.CreateIncomeSourcesResponseHttpParser.{CreateIncomeSourcesResponse, CreateIncomeSourcesResponseHttpReads}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class CreateIncomeSourcesResponseHttpParserSpec extends PlaySpec {

  val reads: HttpResponse => CreateIncomeSourcesResponse = CreateIncomeSourcesResponseHttpReads.read("", "", _)

  "CreateIncomeSourcesResponseHttpReads.read" when {
    s"the API call returns an $NO_CONTENT (NO_CONTENT) status" should {
      "return a create income sources success response" in {
        val httpResponse = HttpResponse(status = NO_CONTENT, json = Json.obj(), headers = Map.empty)
        val result = reads(httpResponse)

        result mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
      }
    }
    "the API call returns an unexpected status" should {
      "return a unexpected status failure response" in {
        val httpResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, json = Json.obj(), headers = Map.empty)
        val result = reads(httpResponse)

        result mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
