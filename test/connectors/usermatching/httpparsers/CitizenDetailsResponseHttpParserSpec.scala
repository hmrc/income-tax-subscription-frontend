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

import connectors.usermatching.httpparsers.CitizenDetailsResponseHttpParser.GetCitizenDetailsHttpReads
import models.usermatching.{CitizenDetailsFailureResponse, CitizenDetails}
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

class CitizenDetailsResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "GET"
  val testUri = "/"
  val testFirstName = Math.random().toString
  val testLastName = Math.random().toString
  val testFullName = Some(testFirstName + " " + testLastName)

  def successResponse(hasUtr: Boolean): JsValue = Json.parse(
    s"""
       |{
       |  "name": {
       |    "current": {
       |      "firstName": "$testFirstName",
       |      "lastName": "$testLastName"
       |    },
       |    "previous": []
       |  },
       |  "ids": {
       |    "nino": "$testNino"${if (hasUtr) s""", "sautr":"$testUtr" """ else ""}
       |  },
       |  "dateOfBirth": "11121971"
       |}
    """.stripMargin
  )

  "GetCitizenDetailsHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response as a Some(CitizenDetailsSuccess) which inludes a utr in its response" in {
        val httpResponse = HttpResponse(OK, json = successResponse(hasUtr = true), Map.empty)

        val res = GetCitizenDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.value mustBe Some(CitizenDetails(Some(testUtr), testFullName))
      }

      "parse a correctly formatted OK response as a Some(None) which does not inlude a utr in its response" in {
        val httpResponse = HttpResponse(OK, json = successResponse(hasUtr = false), Map.empty)

        val res = GetCitizenDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.value mustBe Some(CitizenDetails(None, testFullName))
      }

      "parse a 404 response as None" in {
        val httpResponse = HttpResponse(NOT_FOUND, "")

        val res = GetCitizenDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.value mustBe None
      }

      "parse any other http status as a CitizenDetailsFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST, "")

        val res = GetCitizenDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe CitizenDetailsFailureResponse(BAD_REQUEST)
      }
    }
  }
}
