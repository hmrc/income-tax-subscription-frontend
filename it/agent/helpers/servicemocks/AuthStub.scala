/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import _root_.agent.common.Constants._
import _root_.agent.helpers.IntegrationTestConstants._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}

object AuthStub extends WireMockMethods {
  private val authIDs = "/uri/to/ids"
  private val authoriseUri = "/auth/authorise"

  def stubAuthSuccess(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = OK, body = successfulAuthResponse(AffinityGroup.Agent, ConfidenceLevel.L200, arnEnrolment))
  }

  def stubUnauthorised(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = UNAUTHORIZED)
  }

  val loggedInAt: Some[DateTime] = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
  val previouslyLoggedInAt: Some[DateTime] = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))

  private val arnEnrolment = Json.obj(
    "key" -> agentServiceEnrolmentName,
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> agentServiceIdentifierKey,
        "value" -> testARN
      )
    )
  )

  private def successfulAuthResponse(affinityGroup: AffinityGroup, confidenceLevel: ConfidenceLevel, enrolments: JsObject*): JsObject =
  //Written out manually as the json writer for Enrolment doesn't match the reader
    Json.obj(
      "allEnrolments" -> enrolments,
      "affinityGroup" -> affinityGroup,
      "confidenceLevel" -> confidenceLevel
    )
}
