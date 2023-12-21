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

package connectors

import connectors.agent.UsersGroupsSearchConnector
import connectors.agent.httpparsers.GetUsersForGroupHttpParser.UsersFound
import connectors.stubs.UsersGroupsSearchStub._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import play.api.http.Status.NON_AUTHORITATIVE_INFORMATION
import uk.gov.hmrc.auth.core.{Assistant, User}
import uk.gov.hmrc.http.HeaderCarrier

class UsersGroupsSearchISpec extends ComponentSpecBase {

  lazy val connector: UsersGroupsSearchConnector = app.injector.instanceOf[UsersGroupsSearchConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "GetUserIds" should {
    "Return UsersFound and a Set of User IDs" when {
      "EnrolmentStoreProxy ES0 returns OK and Json response" in {
        stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, successfulResponseBody)

        val res = connector.getUsersForGroup(testGroupId)

        res.futureValue mustBe Right(UsersFound(Map(testCredentialId -> User, testCredentialId2 -> Assistant)))
      }
    }
  }
}
