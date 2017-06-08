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
import helpers.{IntegrationTestConstants, WiremockHelper}
import models.auth.UserIds
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}

object AuthStub extends WireMockMethods {
  val authIDs = "/uri/to/ids"
  val authority = "/auth/authority"

  val gatewayID = "12345"
  val internalID = "internal"
  val externalID = "external"
  val userIDs = UserIds(internalId = internalID, externalId = externalID)

  def stubAuthSuccess(): StubMapping = {
    stubAuthoritySuccess()
    stubAuthorityUserIDsSuccess()
  }

  def stubAuthoritySuccess(): StubMapping =
    when(method = GET, uri = authority)
      .thenReturn(status = OK, body = successfulAuthResponse)

  def successfulAuthResponse: JsObject = {
    Json.obj(
      "uri" -> "/auth/oid/58a2e8c82e00008c005d4699",
      "userDetailsLink" -> "/uri/to/user-details",
      "credentials" -> Json.obj(
        "gatewayId" -> gatewayID
      ),
      "ids" -> authIDs
    )
  }

  def stubAuthorityUserIDsSuccess(): StubMapping =
    when(method = GET, uri = authIDs)
      .thenReturn(status = OK, body = userIDs)

}
