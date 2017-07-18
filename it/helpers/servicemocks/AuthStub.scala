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

package helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.Constants._
import helpers.IntegrationTestConstants._
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}

object AuthStub extends WireMockMethods {
  private val authIDs = "/uri/to/ids"
  private val authoriseUri = "/auth/authorise"

  def stubAuthSuccess(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = OK, body = successfulAuthResponse(ninoEnrolment))
  }

  def stubEnrolled(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = OK, body = successfulAuthResponse(ninoEnrolment, mtdidEnrolment))
  }

  def stubUnauthorised(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = UNAUTHORIZED)
  }

  private val ninoEnrolment = Json.obj(
    "key" -> ninoEnrolmentName,
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> ninoEnrolmentIdentifierKey,
        "value" -> testNino
      )
    )
  )

  private val mtdidEnrolment = Json.obj(
    "key" -> mtdItsaEnrolmentName,
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> mtdItsaEnrolmentIdentifierKey,
        "value" -> testMTDID
      )
    )
  )

  private def successfulAuthResponse(enrolments: JsObject*): JsObject =
  //Written out manually as the json writer for Enrolment doesn't match the reader
    Json.obj(
      "allEnrolments" -> enrolments
    )
}
