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

package connectors


import connectors.agent.EnrolmentStoreProxyConnector
import connectors.agent.httpparsers.AllocateEnrolmentResponseHttpParser.{EnrolFailure, EnrolSuccess}
import connectors.agent.httpparsers.AssignEnrolmentToUserHttpParser.{EnrolmentAssigned, EnrolmentAssignmentFailure}
import connectors.agent.httpparsers.EnrolmentStoreProxyHttpParser.{EnrolmentAlreadyAllocated, EnrolmentNotAllocated, EnrolmentStoreProxyFailure}
import connectors.agent.httpparsers.QueryUsersHttpParser.{EnrolmentStoreProxyConnectionFailure, InvalidJson, NoUsersFound, UsersFound, principalUserIdKey}
import connectors.agent.httpparsers.UpsertEnrolmentResponseHttpParser.{UpsertEnrolmentFailure, UpsertEnrolmentSuccess}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.testIRSAEnrolmentKey
import helpers.servicemocks.EnrolmentStoreProxyStub._
import models.common.subscription.EnrolmentKey
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.Constants.{utrEnrolmentIdentifierKey, utrEnrolmentName}


class EnrolmentStoreProxyConnectorISpec extends ComponentSpecBase {

  lazy val connector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "GetUserIds" should {
    "Return UsersFound and a Set of User IDs" when {
      "EnrolmentStoreProxy ES0 returns OK and Json response" in {
        stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2, testCredentialId3))

        val res = connector.getUserIds(testUtr)

        await(res) mustBe Right(UsersFound(Set(testCredentialId, testCredentialId2, testCredentialId3)))
      }
    }

    "Return InvalidJson" when {
      "EnrolmentStoreProxy ES0 returns OK but invalid Json response" in {
        stubGetUserIds(testUtr)(OK)

        val res = connector.getUserIds(testUtr)

        await(res) mustBe Left(InvalidJson)
      }
    }

    "Return NoUsersFound" when {
      "EnrolmentStoreProxy ES0 returns No Content" in {
        stubGetUserIds(testUtr)(NO_CONTENT)

        val res = connector.getUserIds(testUtr)

        await(res) mustBe Right(NoUsersFound)
      }
    }

    "Return EnrolmentStoreProxyConnectionFailure and status" when {
      "EnrolmentStoreProxy ES0 returns Bad Request" in {
        stubGetUserIds(testUtr)(BAD_REQUEST)

        val res = connector.getUserIds(testUtr)

        await(res) mustBe Left(EnrolmentStoreProxyConnectionFailure(BAD_REQUEST))
      }
    }
  }

  "GetAllocatedEnrolments" should {
    "Return EnrolmentAlreadyAllocated" when {
      "EnrolmentStoreProxy ES1 returns an OK and Json Response" in {
        stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(OK)

        val res = connector.getAllocatedEnrolments(EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr))

        await(res) mustBe Right(EnrolmentAlreadyAllocated(testGroupId))
      }
    }

    "Return EnrolmentNotAllocated" when {
      "EnrolmentStoreProxy ES1 returns No Content" in {
        stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(NO_CONTENT)

        val res = connector.getAllocatedEnrolments(EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr))

        await(res) mustBe Right(EnrolmentNotAllocated)
      }
    }

    "Return EnrolmentStoreProxyFailure and status code" when {
      "EnrolmentStoreProxy ES1 returns Bad Request" in {
        stubGetAllocatedEnrolmentStatus(testIRSAEnrolmentKey)(BAD_REQUEST)

        val res = connector.getAllocatedEnrolments(EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> testUtr))

        await(res) mustBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))
      }
    }
  }


  "allocateEnrolmentWithoutKnownFacts" when {
    "Enrolment Store Proxy returns a Created" should {
      "return an EnrolSuccess" in {
        stubAllocateEnrolmentWithoutKnownFacts(testMtdId, testGroupId, testCredentialId)(CREATED)

        val res = connector.allocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testMtdId)

        await(res) mustBe Right(EnrolSuccess)
      }
    }

    "Enrolment Store Proxy returns a Bad Request" should {
      "return an EnrolFailure" in {
        stubAllocateEnrolmentWithoutKnownFacts(testMtdId, testGroupId, testCredentialId)(BAD_REQUEST)

        val res = connector.allocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testMtdId)

        await(res) mustBe Left(EnrolFailure(""))
      }
    }
  }

  "assignEnrolment" when {
    "Enrolment Store Proxy returns a Created" should {
      "return an EnrolSuccess" in {
        stubAssignEnrolment(testMtdId, userId = testCredentialId)(CREATED)

        val res = connector.assignEnrolment(testCredentialId, testMtdId)

        await(res) mustBe Right(EnrolmentAssigned)
      }
    }

    "Enrolment Store Proxy returns a Bad Request" should {
      "return an EnrolFailure" in {
        stubAssignEnrolment(testMtdId, userId = testCredentialId)(BAD_REQUEST)

        val res = connector.assignEnrolment(testCredentialId, testMtdId)

        await(res) mustBe Left(EnrolmentAssignmentFailure(BAD_REQUEST, ""))
      }
    }
  }


  "upsertEnrolment" when {
    "Enrolment Store Proxy returns a successful response" should {
      "return an EnrolSuccess" in {
        stubUpsertEnrolment(testMtdId, testNino)(NO_CONTENT)

        val res = connector.upsertEnrolment(testMtdId, testNino)

        await(res) mustBe Right(UpsertEnrolmentSuccess)
      }
    }

    "Enrolment Store Proxy returns an unsuccessful response" should {
      "return an EnrolFailure" in {
        stubUpsertEnrolment(testMtdId, testNino)(BAD_REQUEST)

        val res = connector.upsertEnrolment(testMtdId, testNino)

        await(res) mustBe Left(UpsertEnrolmentFailure(BAD_REQUEST, ""))
      }
    }
  }
}

