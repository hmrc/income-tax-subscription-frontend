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

import connectors.httpparser.SignUpResponseHttpParser.{SignUpResponse, SignUpResponseHttpReads}
import models.common.subscription.{SignUpFailureResponse, SignUpSuccessResponse}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class SignUpResponseHttpParserSpec extends PlaySpec {

  val reads: HttpResponse => SignUpResponse = SignUpResponseHttpReads.read("", "", _)
  val testMTDBSA: String = "test-mtdbsa"

  "SignUpResponseHttpReads.read" when {
    s"the API call returns an $OK (OK) status" should {
      "return a sign up successful response" when {
        "the response json could be parsed correctly" in {
          val httpResponse = HttpResponse(status = OK, json = Json.obj("mtdbsa" -> testMTDBSA), headers = Map.empty)
          val result = reads(httpResponse)

          result mustBe Right(SignUpSuccessResponse.SignUpSuccessful(testMTDBSA))
        }
      }
      "return an invalid json failure response" when {
        "the response json could not be parsed correctly" in {
          val httpResponse = HttpResponse(status = OK, json = Json.obj(), headers = Map.empty)
          val result = reads(httpResponse)

          result mustBe Left(SignUpFailureResponse.InvalidJson)
        }
      }
    }
    s"the API call returns an $UNPROCESSABLE_ENTITY (UNPROCESSABLE_ENTITY) status" should {
      "return an already signed up response" in {
        val httpResponse = HttpResponse(status = UNPROCESSABLE_ENTITY, json = Json.obj(), headers = Map.empty)
        val result = reads(httpResponse)

        result mustBe Right(SignUpSuccessResponse.AlreadySignedUp)
      }
    }
    "the API call returns an unexpected status" should {
      "return a unexpected status failure response" in {
        val httpResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, json = Json.obj(), headers = Map.empty)
        val result = reads(httpResponse)

        result mustBe Left(SignUpFailureResponse.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
