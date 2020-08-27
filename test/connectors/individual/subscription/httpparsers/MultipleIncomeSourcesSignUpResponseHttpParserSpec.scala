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

package connectors.individual.subscription.httpparsers

import connectors.individual.subscription.httpparsers.MultipleIncomeSourcesSignUpResponseHttpParser.PostMultipleIncomeSourcesSignUpResponseHttpReads
import models.individual.subscription.{BadlyFormattedSignUpResponse, MultipleIncomeSourcesSignUpFailure}
import models.individual.subscription.{MultipleIncomeSourcesSignUpFailureResponse, MultipleIncomeSourcesSignUpSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

class MultipleIncomeSourcesSignUpResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"
  val testUri = "/"

  "MultipleIncomeSourcesSignUpResponseHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response as a MultipleIncomeSourcesSignUpSuccess" in {
        val httpResponse = HttpResponse(OK, Json.toJson(MultipleIncomeSourcesSignUpSuccess(testMTDID)))

        val res = PostMultipleIncomeSourcesSignUpResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe MultipleIncomeSourcesSignUpSuccess(testMTDID)
      }

      "parse an incorrectly formatted OK response as a BadlyFormattedSignUpResponse" in {
        val httpResponse = HttpResponse(OK, Json.obj())

        val res = PostMultipleIncomeSourcesSignUpResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe BadlyFormattedSignUpResponse
      }

      "parse any other http status as a MultipleIncomeSourcesSignUpFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST)

        val res = PostMultipleIncomeSourcesSignUpResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe MultipleIncomeSourcesSignUpFailureResponse(BAD_REQUEST)
      }
    }
  }
}
