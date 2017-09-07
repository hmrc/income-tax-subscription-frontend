/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors.httpparsers

import connectors.httpparsers.MatchUserHttpParser.MatchUserHttpReads
import connectors.models.matching.{UserMatchFailureResponseModel, UserMatchSuccessResponseModel, UserMatchUnexpectedError}
import org.scalatest.EitherValues
import play.api.libs.json.{JsError, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HttpResponse
import utils.TestConstants._
import utils.TestModels._
import utils.UnitTestTrait

class MatchUserHttpParserSpec extends UnitTestTrait with EitherValues {
  "MatchClientHttpReads" when {
    "read" should {
      val testUrl = "/"
      val testMethod = "get"

      "return user details if authenticator response with OK and valid JSON" in {
        val testUserDetailsMatch = UserMatchSuccessResponseModel(
          firstName = testFirstName,
          lastName = testLastName,
          dateOfBirth = testStartDate.toString,
          nino = testNino,
          saUtr = None
        )

        val httpResponse = HttpResponse(responseStatus = OK, responseJson = Json.toJson(testUserDetailsMatch))

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse).right.value mustBe Some(testUserDetailsMatch)
      }

      "return error if authenticator returns an invalid json" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(
          responseStatus = OK,
          responseJson = Some(json)
        )

        val expectedJsError = json.validate[UserMatchSuccessResponseModel].asInstanceOf[JsError]

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse).left.value mustBe UserMatchFailureResponseModel(expectedJsError)
      }

      "return none if authenticator returns an UNAUTHORIZED without an unexpected error" in {
        val httpResponse = HttpResponse(
          responseStatus = UNAUTHORIZED,
          responseJson = Some(Json.toJson(UserMatchFailureResponseModel(testErrorMessage)))
        )

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse).right.value mustBe empty
      }

      "return an error if authenticator returns an UNAUTHORIZED response that cannot be parsed" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(
          responseStatus = UNAUTHORIZED,
          responseJson = Some(json)
        )

        val expectedJsError = json.validate[UserMatchFailureResponseModel].asInstanceOf[JsError]

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse).left.value mustBe UserMatchFailureResponseModel(expectedJsError)
      }

      "return an error if authenticator returns an UNAUTHORIZED response that contains an unexpected error" in {
        val json = Json.obj()

        val httpResponse = HttpResponse(
          responseStatus = UNAUTHORIZED,
          responseJson = Some(Json.toJson(UserMatchUnexpectedError))
        )

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse).left.value mustBe UserMatchUnexpectedError
      }

      "return error if authenticator returns another status" in {
        val httpResponse = HttpResponse(responseStatus = BAD_REQUEST)

        MatchUserHttpReads.read(testMethod, testUrl, httpResponse).left.value mustBe UserMatchFailureResponseModel(httpResponse)
      }
    }
  }
}
