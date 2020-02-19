
package helpers.agent.servicemocks

import agent.common.Constants._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.agent.IntegrationTestConstants._
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
