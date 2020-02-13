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

package connectors.individual.business.httpparsers

import connectors.individual.business.httpparsers.AddressLookupResponseHttpParser._
import core.utils.UnitTestTrait
import models.individual.business.address._
import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.{JsNull, Json}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HttpResponse

class AddressLookupResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"
  val testUri = "/"

  val testRedirectionUrl = "url"

  "InitAddressLookupHttpReads.read" should {
    "parse an ACCEPTED response and return the url in its response" in {
      val httpResponse = HttpResponse(ACCEPTED, JsNull, responseHeaders = Map(HeaderNames.LOCATION -> Seq(testRedirectionUrl)))

      val res = InitAddressLookupHttpReads.read(testHttpVerb, testUri, httpResponse)

      res.right.value mustBe testRedirectionUrl
    }

    "parse any other http status as a AddressLookupFailureResponse" in {
      val httpResponse = HttpResponse(BAD_REQUEST)

      val res = InitAddressLookupHttpReads.read(testHttpVerb, testUri, httpResponse)

      res.left.value mustBe AddressLookupInitFailureResponse(BAD_REQUEST)
    }
  }

  "ConfirmAddressLookupHttpReads.read" should {

    val testAuditRef = "3edfd6a9-7543-46f7-867e-dfa1bb217dd8"
    val testId = "GB990091234513"
    val testLine1 = "11 Test Street"
    val testLine2 = "Testtown"
    val testPostCode = "ZZ11 1ZZ"
    val testCountryCode = "GB"
    val testCountryName = "United Kingdom"

    lazy val testJson = Json.parse(
      s"""{
         |"auditRef":"$testAuditRef",
         |"id":"$testId",
         |"address":{"lines":["$testLine1","$testLine2"],"postcode":"$testPostCode","country":{"code":"$testCountryCode","name":"$testCountryName"}}
         |}""".stripMargin)

    "parse an validly formatted address correctly" in {
      val httpResponse = HttpResponse(OK, testJson)

      val res = ConfirmAddressLookupHttpReads.read(testHttpVerb, testUri, httpResponse)

      res.right.value mustBe ReturnedAddress(
        auditRef = testAuditRef,
        id = testId,
        address = Address(
          lines = Some(List(testLine1, testLine2)),
          postcode = Some(testPostCode),
          country = Some(Country(testCountryCode, testCountryName))
        )
      )
    }

    "return MalformatAddressReturned if the returned json is invalid" in {
      val httpResponse = HttpResponse(OK, JsNull)

      val res = ConfirmAddressLookupHttpReads.read(testHttpVerb, testUri, httpResponse)

      res.left.value mustBe MalformatAddressReturned
    }

    "return UnexpectedStatusReturned if the returned unexpected status" in {
      val httpResponse = HttpResponse(BAD_REQUEST, testJson)

      val res = ConfirmAddressLookupHttpReads.read(testHttpVerb, testUri, httpResponse)

      res.left.value mustBe UnexpectedStatusReturned(BAD_REQUEST)
    }

  }
}
