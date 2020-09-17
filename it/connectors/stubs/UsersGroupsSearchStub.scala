
package connectors.stubs

import connectors.agent.httpparsers.GetUsersForGroupHttpParser.CredentialRoleReads._
import connectors.agent.httpparsers.GetUsersForGroupHttpParser.UserReads.{credentialRoleKey, userIdKey}
import helpers.IntegrationTestConstants._
import helpers.servicemocks.WireMockMethods
import play.api.libs.json.{JsArray, JsValue, Json}

object UsersGroupsSearchStub extends WireMockMethods {

  private def getUsersForGroupUrl(groupId: String) = s"/users-groups-search/groups/$groupId/users"

  def stubGetUsersForGroups(groupId: String)(responseStatus: Int, responseBody: JsValue = Json.obj()): Unit = {
    when(
      method = GET,
      uri = getUsersForGroupUrl(groupId)
    ) thenReturn(responseStatus, responseBody)
  }

  val successfulResponseBody: JsArray = Json.arr(
    Json.obj(
      userIdKey -> testCredentialId,
      credentialRoleKey -> AdminKey
    ),
    Json.obj(
      userIdKey -> testCredentialId2,
      credentialRoleKey -> AssistantKey
    )
  )

}
