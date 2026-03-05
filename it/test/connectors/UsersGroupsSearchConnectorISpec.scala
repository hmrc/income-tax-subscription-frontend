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

import connectors.agent.httpparsers.GetUsersForGroupHttpParser.UsersFound
import connectors.httpparser.GetUserDetailsByCredIdHttpParser.{InvalidJson, UnexpectedStatus}
import connectors.stubs.UsersGroupsSearchStub.*
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.*
import models.individual.ObfuscatedIdentifier.{UserEmail, ObfuscatedUserId}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NON_AUTHORITATIVE_INFORMATION}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.{Assistant, User}
import uk.gov.hmrc.http.HeaderCarrier

class UsersGroupsSearchConnectorISpec extends ComponentSpecBase {

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

  "getUserDetailsByCredId" should {
    "return a user details model with an obfuscated user id" when {
      "the identity provider type is SCP" in {
        stubGetUserDetailsByCredId(testCredId)(
          responseStatus = NON_AUTHORITATIVE_INFORMATION,
          responseBody = Json.obj(
            "identityProviderType" -> "SCP",
            "obfuscatedUserId" -> "*****678"
          )
        )

        connector.getUserDetailsByCredId(testCredId).futureValue mustBe Right(ObfuscatedUserId("*****678"))
      }
    }
    "return a user details model with an obfuscated email" when {
      "the identity provider type is ONE_LOGIN" in {
        stubGetUserDetailsByCredId(testCredId)(
          responseStatus = NON_AUTHORITATIVE_INFORMATION,
          responseBody = Json.obj(
            "identityProviderType" -> "ONE_LOGIN",
            "email" -> "test@email.com"
          )
        )

        connector.getUserDetailsByCredId(testCredId).futureValue mustBe Right(UserEmail("test@email.com"))
      }
    }
    "return an InvalidJson error" when {
      "the response could not be parsed" in {
        stubGetUserDetailsByCredId(testCredId)(NON_AUTHORITATIVE_INFORMATION)

        connector.getUserDetailsByCredId(testCredId).futureValue mustBe Left(InvalidJson)
      }
    }
    "return an UnexpectedStatus error" when {
      "the response has a status which is not supported" in {
        stubGetUserDetailsByCredId(testCredId)(INTERNAL_SERVER_ERROR)

        connector.getUserDetailsByCredId(testCredId).futureValue mustBe Left(UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
