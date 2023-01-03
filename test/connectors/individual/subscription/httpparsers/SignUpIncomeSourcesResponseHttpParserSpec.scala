/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser.PostMultipleIncomeSourcesSignUpResponseHttpReads
import models.common.subscription.{BadlyFormattedSignUpIncomeSourcesResponse, SignUpIncomeSourcesFailureResponse, SignUpIncomeSourcesSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

class SignUpIncomeSourcesResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"
  val testUri = "/"

  "MultipleIncomeSourcesSignUpResponseHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response as a SignUpIncomeSourcesSuccess" in {
        val httpResponse = HttpResponse(OK, json = Json.toJson(SignUpIncomeSourcesSuccess(testMTDID)), Map.empty)

        val res = PostMultipleIncomeSourcesSignUpResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.value mustBe SignUpIncomeSourcesSuccess(testMTDID)
      }

      "parse an incorrectly formatted OK response as a BadlyFormattedSignUpIncomeSourcesResponse" in {
        val httpResponse = HttpResponse(OK, json = Json.obj(), Map.empty)

        val res = PostMultipleIncomeSourcesSignUpResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe BadlyFormattedSignUpIncomeSourcesResponse
      }

      "parse any other http status as a SignUpIncomeSourcesFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST, "")

        val res = PostMultipleIncomeSourcesSignUpResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe SignUpIncomeSourcesFailureResponse(BAD_REQUEST)
      }
    }
  }
}
