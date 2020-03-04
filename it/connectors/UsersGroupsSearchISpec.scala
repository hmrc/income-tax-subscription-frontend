
package connectors

import connectors.agent.UsersGroupsSearchConnector
import connectors.agent.httpparsers.GetUsersForGroupHttpParser.UsersFound
import helpers.IntegrationTestConstants._
import connectors.stubs.UsersGroupsSearchStub._
import helpers.ComponentSpecBase
import uk.gov.hmrc.auth.core.{Assistant, User}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.http.Status.NON_AUTHORITATIVE_INFORMATION

class UsersGroupsSearchISpec extends ComponentSpecBase {

  lazy val connector: UsersGroupsSearchConnector = app.injector.instanceOf[UsersGroupsSearchConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "GetUserIds" should {
    "Return UsersFound and a Set of User IDs" when {
      "EnrolmentStoreProxy ES0 returns OK and Json response" in {
        stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, successfulResponseBody)

        val res = connector.getUsersForGroup(testGroupId)

        await(res) shouldBe Right(UsersFound(Map(testCredentialId -> User, testCredentialId2 -> Assistant)))
      }
    }
  }
}
