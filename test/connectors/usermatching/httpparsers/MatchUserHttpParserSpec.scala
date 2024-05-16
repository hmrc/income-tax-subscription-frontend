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

package connectors.usermatching.httpparsers

import connectors.usermatching.httpparsers.MatchUserHttpParser.MatchUserHttpReads
import models.usermatching.{UserMatchFailureResponseModel, UserMatchSuccessResponseModel}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import utilities.TestModels._
import utilities.individual.TestConstants._

class MatchUserHttpParserSpec extends PlaySpec {

  "MatchUserHttpReads" when {
    "read" should {
      val testUrl = "/"
      val testMethod = "GET"

      "return user details if authenticator response with OK and valid JSON" in {
        val testUserDetailsMatch = UserMatchSuccessResponseModel(
          firstName = testFirstName,
          lastName = testLastName,
          dateOfBirth = testStartDateThisYear.toString,
          nino = testNino,
          saUtr = None
        )

        val httpResponse = HttpResponse(status = OK, json = Json.toJson(testUserDetailsMatch), Map.empty)

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse) mustBe Right(Some(testUserDetailsMatch))
      }

      "return error if authenticator returns an invalid json" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(
          status = OK,
          json = json,
          headers = Map.empty
        )

        val expectedJsError = json.validate[UserMatchSuccessResponseModel].asInstanceOf[JsError]

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse) mustBe Left(UserMatchFailureResponseModel(expectedJsError))
      }

      "return none if authenticator returns an UNAUTHORIZED without an unexpected error" in {
        val httpResponse = HttpResponse(
          status = UNAUTHORIZED,
          json = Json.toJson(UserMatchFailureResponseModel(testErrorMessage)),
          headers = Map.empty
        )

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse) mustBe Right(None)
      }

      "return none if authenticator returns a FAILED_DEPENDENCY (424) status" in {
        val httpResponse = HttpResponse(
          status = FAILED_DEPENDENCY,
          body = ""
        )

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse) mustBe Right(None)
      }

      "return an error if authenticator returns an UNAUTHORIZED response that cannot be parsed" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(
          status = UNAUTHORIZED,
          json = json,
          headers = Map.empty
        )

        val expectedJsError = json.validate[UserMatchFailureResponseModel].asInstanceOf[JsError]

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse) mustBe Left(UserMatchFailureResponseModel(expectedJsError))
      }

      "return error if authenticator returns another status" in {
        val httpResponse = HttpResponse(BAD_REQUEST, "")

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse) mustBe Left(UserMatchFailureResponseModel(httpResponse))
      }
    }
  }
}
