
package connectors.stubs

import connectors.agent.httpparsers.GetUsersForGroupsHttpParser.UserReads.{credentialRoleKey, userIdKey}
import connectors.agent.httpparsers.GetUsersForGroupsHttpParser.CredentialRoleReads._
import helpers.servicemocks.WireMockMethods
import play.api.libs.json.{JsArray, JsValue, Json}
import helpers.IntegrationTestConstants._

object UsersGroupsSearchStub extends WireMockMethods {

  private def getUsersForGroupUrl(groupId: String) = s"/users-groups-search/groups/$groupId/users"

  def stubGetUsersForGroups(groupId: String)(responseStatus: Int, responseBody: JsValue): Unit = {
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
